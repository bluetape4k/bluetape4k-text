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

            // 알고리즘: prefix를 한 글자씩 늘려가며 정규화합니다.
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
