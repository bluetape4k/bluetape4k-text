package io.bluetape4k.text.search.internal

import io.bluetape4k.text.search.NormalizationForm
import java.text.Normalizer

/**
 * 원본 문자열과 유니코드 정규화 문자열 사이의 양방향 문자 offset 매핑입니다.
 *
 * 유니코드 정규화는 문자열 길이를 바꿀 수 있습니다. 예를 들어 NFC는 자모를 합성하고, NFKC는 일부 합자와
 * 기호를 여러 문자로 확장합니다. 그래서 match offset을 원본 문자열 위치로 되돌리려면 별도 매핑 테이블이
 * 필요합니다.
 *
 * ## 길이 변화 예
 * - **NFC**: `ㄴㅏ`(2 chars, 분리 자모) -> `나`(1 char), 길이가 줄어듭니다.
 * - **NFKC**: `㈜`(1 char) -> `(주)`(3 chars), 길이가 늘어납니다.
 *
 * 정규화 상호작용 구간 내부의 prefix 추적은 [MAX_NORMALIZATION_SEGMENT_LENGTH]자로 제한합니다.
 * 이 제한은 비정상적으로 긴 combining mark 연속 입력이 다시 quadratic 비용을 만들지 않도록 하며,
 * 제한을 넘는 입력은 [IllegalArgumentException]으로 거부합니다.
 *
 * ## Usage pattern
 * ```kotlin
 * val (normalized, mapping) = OffsetMapping.build(originalText, NormalizationForm.NFC)
 * val emits = trie.parseText(normalized)
 * emits.forEach { e ->
 *     val origStart = mapping?.toOriginal(e.start) ?: e.start
 *     val origEnd   = mapping?.toOriginalEndInclusive(e.end) ?: e.end
 * }
 * ```
 *
 * @property normToOrig 정규화 문자열 위치를 원본 문자열 위치로 바꾸는 배열입니다. 크기는 `normLen + 1`이며
 * sentinel을 포함합니다.
 * @property origToNorm 원본 문자열 위치를 정규화 문자열 위치로 바꾸는 배열입니다. 크기는 `origLen + 1`이며
 * sentinel을 포함합니다.
 */
internal class OffsetMapping private constructor(
    private val normToOrig: IntArray,
    private val origToNorm: IntArray,
) {
    /**
     * 정규화 문자열 offset을 대응하는 원본 문자열 offset으로 변환합니다.
     *
     * @param normOffset 정규화 문자열의 inclusive offset입니다.
     * @return 원본 문자열의 대응 inclusive offset입니다.
     */
    fun toOriginal(normOffset: Int): Int =
        when {
            normToOrig.isEmpty() -> 0
            normOffset < 0 -> normToOrig[0]
            normOffset < normToOrig.size -> normToOrig[normOffset]
            else -> normToOrig.last()
        }

    /**
     * 정규화 문자열의 inclusive end offset을 대응하는 원본 문자열의 inclusive end offset으로 변환합니다.
     *
     * `Emit.end`가 inclusive이므로 먼저 exclusive end(`+1`)로 변환한 뒤 1을 뺍니다.
     *
     * @param normEndInclusive 정규화 문자열의 inclusive end offset입니다.
     * @return 원본 문자열의 inclusive end offset입니다.
     */
    fun toOriginalEndInclusive(normEndInclusive: Int): Int {
        val normEndExclusive = normEndInclusive + 1
        val origEndExclusive = if (normEndExclusive < normToOrig.size) {
            normToOrig[normEndExclusive]
        } else {
            normToOrig.last()
        }
        return origEndExclusive - 1
    }

    /**
     * 원본 문자열 offset을 대응하는 정규화 문자열 offset으로 변환합니다.
     *
     * @param origOffset 원본 문자열의 inclusive offset입니다.
     * @return 정규화 문자열의 대응 inclusive offset입니다.
     */
    fun toNormalized(origOffset: Int): Int =
        when {
            origToNorm.isEmpty() -> 0
            origOffset < 0 -> origToNorm[0]
            origOffset < origToNorm.size -> origToNorm[origOffset]
            else -> origToNorm.last()
        }

    companion object {
        /**
         * [original] 문자열과 정규화 [form]으로 [OffsetMapping]을 생성합니다.
         *
         * 문자 단위 sliding normalization으로 위치별 offset 변화를 추적합니다. 생성되는 정규화 문자열은
         * `Normalizer.normalize(original, form)` 결과와 같습니다.
         *
         * @param original 정규화할 원본 [CharSequence]입니다.
         * @param form 적용할 정규화 형식입니다.
         * @return `(normalizedString, OffsetMapping?)` pair입니다. [form]이 [NormalizationForm.NONE]이면
         * mapping은 `null`입니다.
         */
        fun build(original: CharSequence, form: NormalizationForm): Pair<String, OffsetMapping?> {
            if (form == NormalizationForm.NONE) {
                return original.toString() to null
            }

            val javaForm = when (form) {
                NormalizationForm.NFC -> Normalizer.Form.NFC
                NormalizationForm.NFKC -> Normalizer.Form.NFKC
                NormalizationForm.NONE -> return original.toString() to null
            }

            val origLen = original.length
            val origToNorm = IntArray(origLen + 1)

            // 빈 입력 단축
            if (origLen == 0) {
                val emptyNorm = IntArray(1)  // [0] sentinel만 둡니다.
                origToNorm[0] = 0
                return "" to OffsetMapping(emptyNorm, origToNorm)
            }

            // 정규화 상호작용이 가능한 구간(normalization segment)별로 prefix를 추적합니다.
            // ASCII처럼 각 문자가 독립적인 입력은 한 문자 segment가 되므로 전체 비용이 O(n)입니다.
            // combining mark, Hangul Jamo, NFKC 반각 voiced mark는 이전 문자와 같은 segment에
            // 남겨 정규화 결과와 기존의 "마지막 기여 문자" 매핑을 보존합니다.
            val normToOrigList = ArrayList<Int>(origLen)
            val normalizedBuilder = StringBuilder(origLen)
            var segmentStart = 0
            var index = 0
            var previousCodePoint = -1

            while (index < origLen) {
                val codePoint = Character.codePointAt(original, index)
                if (index > segmentStart && !continuesNormalization(previousCodePoint, codePoint, form)) {
                    appendSegment(
                        original = original,
                        start = segmentStart,
                        end = index,
                        javaForm = javaForm,
                        normalized = normalizedBuilder,
                        normToOrigList = normToOrigList,
                        origToNorm = origToNorm,
                    )
                    segmentStart = index
                }
                previousCodePoint = codePoint
                index += Character.charCount(codePoint)
            }

            appendSegment(
                original = original,
                start = segmentStart,
                end = origLen,
                javaForm = javaForm,
                normalized = normalizedBuilder,
                normToOrigList = normToOrigList,
                origToNorm = origToNorm,
            )

            val normalized = normalizedBuilder.toString()
            origToNorm[origLen] = normalized.length

            // normToOrig 배열 + sentinel
            val normToOrig = IntArray(normalized.length + 1)
            for (i in normalized.indices) normToOrig[i] = normToOrigList[i]
            normToOrig[normalized.length] = origLen

            return normalized to OffsetMapping(normToOrig, origToNorm)
        }

        private fun appendSegment(
            original: CharSequence,
            start: Int,
            end: Int,
            javaForm: Normalizer.Form,
            normalized: StringBuilder,
            normToOrigList: MutableList<Int>,
            origToNorm: IntArray,
        ) {
            val segmentLength = end - start
            require(segmentLength <= MAX_NORMALIZATION_SEGMENT_LENGTH) {
                "normalization segment too long: $segmentLength chars (max $MAX_NORMALIZATION_SEGMENT_LENGTH)"
            }
            val segment = StringBuilder(segmentLength)
            var lastNormLen = 0

            for (localPos in 0 until segmentLength) {
                val origPos = start + localPos
                origToNorm[origPos] = normalized.length + lastNormLen
                segment.append(original[origPos])
                val curLen = Normalizer.normalize(segment, javaForm).length
                when {
                    curLen > lastNormLen -> repeat(curLen - lastNormLen) { normToOrigList.add(origPos) }
                    curLen < lastNormLen -> {
                        repeat(lastNormLen - curLen) { normToOrigList.removeAt(normToOrigList.lastIndex) }
                        if (normToOrigList.isNotEmpty()) normToOrigList[normToOrigList.lastIndex] = origPos
                    }
                    else -> if (normToOrigList.isNotEmpty()) normToOrigList[normToOrigList.lastIndex] = origPos
                }
                lastNormLen = curLen
            }

            val normalizedSegment = Normalizer.normalize(segment, javaForm)
            check(normalizedSegment.length == lastNormLen) {
                "internal: normalized segment length mismatch (${normalizedSegment.length} != $lastNormLen)"
            }
            normalized.append(normalizedSegment)
            origToNorm[end] = normalized.length
        }

        private fun continuesNormalization(previous: Int, current: Int, form: NormalizationForm): Boolean =
            isCombiningMark(current) ||
                isHangulContinuation(previous, current) ||
                (form == NormalizationForm.NFKC && current in COMPATIBILITY_COMBINING_MARKS)

        private fun isCombiningMark(codePoint: Int): Boolean = when (Character.getType(codePoint)) {
            Character.NON_SPACING_MARK.toInt(),
            Character.COMBINING_SPACING_MARK.toInt(),
            Character.ENCLOSING_MARK.toInt(),
            -> true

            else -> false
        }

        private fun isHangulContinuation(previous: Int, current: Int): Boolean =
            (previous in HANGUL_LEADING_JAMO && current in HANGUL_VOWEL_JAMO) ||
                (previous in HANGUL_VOWEL_JAMO && current in HANGUL_TRAILING_JAMO) ||
                (previous in HANGUL_LV_SYLLABLES && current in HANGUL_TRAILING_JAMO)

        private const val HANGUL_SYLLABLE_BASE = 0xAC00
        private const val HANGUL_SYLLABLE_COUNT = 11_172
        private const val MAX_NORMALIZATION_SEGMENT_LENGTH = 1_024
        private val HANGUL_LEADING_JAMO = 0x1100..0x1112
        private val HANGUL_VOWEL_JAMO = 0x1161..0x1175
        private val HANGUL_TRAILING_JAMO = 0x11A8..0x11C2
        private val HANGUL_LV_SYLLABLES = HANGUL_SYLLABLE_BASE until
            (HANGUL_SYLLABLE_BASE + HANGUL_SYLLABLE_COUNT) step 28
        private val COMPATIBILITY_COMBINING_MARKS = setOf(0xFF9E, 0xFF9F)

        /**
         * `normOffset == origOffset`인 identity mapping을 반환합니다. 정규화를 적용하지 않은 경우에 사용합니다.
         *
         * @param length 매핑할 문자열 길이입니다.
         * @return 원본 offset과 정규화 offset이 같은 [OffsetMapping]입니다.
         */
        fun identity(length: Int): OffsetMapping {
            val arr = IntArray(length + 1) { it }
            return OffsetMapping(arr.copyOf(), arr.copyOf())
        }
    }
}
