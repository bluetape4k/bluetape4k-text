package io.bluetape4k.text.search.internal

import io.bluetape4k.text.search.NormalizationForm
import java.text.Normalizer

/**
 * Bidirectional char-offset mapping between original and Unicode-normalized text.
 *
 * Unicode normalization can change string length (e.g. NFC composes jamo, NFKC expands ligatures),
 * so a mapping table is required to convert match offsets back to original positions.
 *
 * ## Length-change examples
 * - **NFC**: `ㄴㅏ` (2 chars, decomposed jamo) → `나` (1 char) — length decreases
 * - **NFKC**: `㈜` (1 char) → `(주)` (3 chars) — length increases
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
 * @property normToOrig normalized-text position → original-text position (size = normLen + 1, includes sentinel)
 * @property origToNorm original-text position → normalized-text position (size = origLen + 1, includes sentinel)
 */
internal class OffsetMapping private constructor(
    private val normToOrig: IntArray,
    private val origToNorm: IntArray,
) {
    /**
     * Converts a normalized-text offset to the corresponding original-text offset.
     *
     * @param normOffset inclusive offset in the normalized text
     * @return corresponding inclusive offset in the original text
     */
    fun toOriginal(normOffset: Int): Int =
        when {
            normToOrig.isEmpty() -> 0
            normOffset < 0 -> normToOrig[0]
            normOffset < normToOrig.size -> normToOrig[normOffset]
            else -> normToOrig.last()
        }

    /**
     * Converts a normalized-text inclusive end offset to the corresponding original-text inclusive end offset.
     *
     * Because `Emit.end` is inclusive, this helper converts via exclusive end (`+1`) then subtracts 1.
     *
     * @param normEndInclusive inclusive end offset in the normalized text
     * @return inclusive end offset in the original text
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
     * Converts an original-text offset to the corresponding normalized-text offset.
     *
     * @param origOffset inclusive offset in the original text
     * @return corresponding inclusive offset in the normalized text
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
         * Builds an [OffsetMapping] from [original] text and a normalization [form].
         *
         * Uses char-by-char sliding normalization to track per-position offset deltas.
         * The resulting normalized string is identical to `Normalizer.normalize(original, form)`.
         *
         * @param original original [CharSequence]
         * @param form normalization form to apply
         * @return `(normalizedString, OffsetMapping?)` — mapping is `null` when [form] is [NormalizationForm.NONE]
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
                val emptyNorm = IntArray(1)  // [0] sentinel only
                origToNorm[0] = 0
                return "" to OffsetMapping(emptyNorm, origToNorm)
            }

            // 알고리즘: incremental prefix normalization.
            //
            // 매 단계마다 원본 prefix를 한 글자 늘려가며 전체 prefix를 normalize.
            // 정규화 결과 길이의 변화 패턴으로 origPos를 normalized 위치에 매핑한다:
            //
            // - 길이 증가 (확장): 새로 늘어난 normalized 위치는 모두 현재 origPos에서 비롯됨
            // - 길이 감소 (합성): 일부 trailing 위치가 사라짐. 남은 마지막 위치는 합성 결과 → 현재 origPos가 마지막 기여자
            // - 길이 동일: 마지막 위치가 reorder/replacement으로 갱신되었을 수 있으므로 origPos 재기록
            //
            // 결과는 `Normalizer.normalize(original, form)`과 정확히 동일하다 (생성 방식 동일).
            //
            // 복잡도: 최악의 경우 O(n²) — 매 step마다 prefix 전체 정규화.
            // text-search 입력 크기에선 충분하나, 대용량은 ICU4J `Normalizer2.normalizeSecondAndAppend` 권장.
            val sb = StringBuilder(origLen)
            val normToOrigList = ArrayList<Int>(origLen)
            var lastNormLen = 0

            for (origPos in 0 until origLen) {
                origToNorm[origPos] = lastNormLen
                sb.append(original[origPos])
                val curNorm = Normalizer.normalize(sb, javaForm)
                val curLen = curNorm.length
                when {
                    curLen > lastNormLen -> {
                        // 확장: 새로 추가된 normalized 위치는 모두 origPos가 기여
                        repeat(curLen - lastNormLen) { normToOrigList.add(origPos) }
                    }
                    curLen < lastNormLen -> {
                        // 합성으로 길이 감소: 잉여 trailing 매핑 제거.
                        // 남은 마지막 위치는 현재 origPos(합성을 완성한 마지막 기여자)로 갱신.
                        while (normToOrigList.size > curLen) {
                            normToOrigList.removeAt(normToOrigList.size - 1)
                        }
                        if (normToOrigList.isNotEmpty()) {
                            normToOrigList[normToOrigList.size - 1] = origPos
                        }
                    }
                    else -> {
                        // 길이 동일: 합성/재배열로 마지막 normalized 문자가 교체됨.
                        // 마지막 위치를 현재 origPos(합성을 완성한 마지막 기여자)로 갱신.
                        if (normToOrigList.isNotEmpty()) {
                            normToOrigList[normToOrigList.size - 1] = origPos
                        }
                    }
                }
                lastNormLen = curLen
            }
            origToNorm[origLen] = lastNormLen

            // normalized 결과 — sb 정규화 결과를 그대로 사용
            val normalized = Normalizer.normalize(sb, javaForm)
            check(normalized.length == lastNormLen) {
                "internal: normalized length mismatch (${normalized.length} != $lastNormLen)"
            }

            // normToOrig 배열 + sentinel
            val normToOrig = IntArray(lastNormLen + 1)
            for (i in 0 until lastNormLen) normToOrig[i] = normToOrigList[i]
            normToOrig[lastNormLen] = origLen

            return normalized to OffsetMapping(normToOrig, origToNorm)
        }

        /**
         * Returns an identity mapping where `normOffset == origOffset` (no normalization applied).
         *
         * @param length length of the text to map
         */
        fun identity(length: Int): OffsetMapping {
            val arr = IntArray(length + 1) { it }
            return OffsetMapping(arr.copyOf(), arr.copyOf())
        }
    }
}
