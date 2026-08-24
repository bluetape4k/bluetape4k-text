package io.bluetape4k.text.search.internal

import io.bluetape4k.text.search.NormalizationForm
import io.bluetape4k.text.search.SearchOptions
import java.text.Normalizer

/**
 * 입력 문자열에 [SearchOptions] 정규화 파이프라인을 적용합니다.
 *
 * 적용 순서는 다음과 같습니다.
 * 1. [SearchOptions.normalization]에 따른 유니코드 정규화(NFC 또는 NFKC)
 * 2. [SearchOptions.ignoreCase]가 `true`일 때 Unicode 소문자 변환
 *
 * 일관된 match를 보장하려면 키워드 등록 시점과 검색 시점에 같은 방식으로 적용해야 합니다.
 *
 * ```kotlin
 * val opts = SearchOptions(ignoreCase = true, normalization = NormalizationForm.NFC)
 * val normalized = applyPipeline("APPLE", opts) // "apple"
 * ```
 *
 * @param s 정규화할 입력 문자열입니다.
 * @param opts 정규화 파이프라인을 정의하는 검색 옵션입니다.
 * @return 정규화된 문자열입니다. 옵션에 따라 소문자 변환도 적용됩니다.
 */
internal fun applyPipeline(s: CharSequence, opts: SearchOptions): String {
    val step1 = when (opts.normalization) {
        NormalizationForm.NONE -> s.toString()
        NormalizationForm.NFC -> Normalizer.normalize(s, Normalizer.Form.NFC)
        NormalizationForm.NFKC -> Normalizer.normalize(s, Normalizer.Form.NFKC)
    }
    return if (opts.ignoreCase) step1.lowercaseCharByChar() else step1
}

/**
 * 검색 keyword와 text에 동일하게 적용하는 Unicode 소문자 변환입니다.
 *
 * [String.lowercase]는 `İ`처럼 한 code point를 여러 UTF-16 문자로 확장하고, 그리스어 sigma처럼
 * 문맥을 반영합니다. keyword와 text 모두 같은 변환을 사용해야 대소문자 무시 검색이 일관됩니다.
 */
internal fun String.lowercaseCharByChar(): String = lowercase()

/**
 * 소문자 변환 결과와 원본 UTF-16 offset 매핑입니다.
 *
 * 소문자 변환으로 길이가 늘어날 수 있으므로 시작 offset과 종료 경계를 별도로 보관합니다. 예를 들어
 * `İ`의 `i\u0307` 두 code unit은 모두 원본 `İ` 하나의 범위로 복원됩니다.
 */
internal class CaseFoldedText(
    val text: String,
    private val startToSource: IntArray,
    private val endToSourceExclusive: IntArray,
) {
    fun toSourceStart(offset: Int): Int = when {
        startToSource.isEmpty() -> 0
        offset < 0 -> startToSource[0]
        offset < startToSource.size -> startToSource[offset]
        else -> startToSource.last()
    }

    fun toSourceEndInclusive(offset: Int): Int {
        val exclusiveOffset = offset + 1
        val sourceExclusive = when {
            endToSourceExclusive.isEmpty() -> 0
            exclusiveOffset < endToSourceExclusive.size -> endToSourceExclusive[exclusiveOffset]
            else -> endToSourceExclusive.last()
        }
        return sourceExclusive - 1
    }
}

/**
 * Unicode 소문자 변환을 수행하면서 결과 UTF-16 단위를 원본 code point 범위에 연결합니다.
 *
 * `String.lowercase()`의 전체 문자열 문맥(예: final sigma)은 유지하고, 일반적인 code point별
 * 확장(`İ`, supplementary Deseret 등)은 정확한 source 범위로 매핑합니다.
 */
internal fun String.lowercaseWithMapping(): CaseFoldedText {
    val folded = lowercase()
    val startToSource = IntArray(folded.length + 1)
    val endToSourceExclusive = IntArray(folded.length + 1)
    var sourceIndex = 0
    var foldedIndex = 0

    while (sourceIndex < length) {
        val codePoint = Character.codePointAt(this, sourceIndex)
        val sourceEnd = sourceIndex + Character.charCount(codePoint)
        val codePointText = substring(sourceIndex, sourceEnd)
        val independentFold = codePointText.lowercase()
        val actualFold = when {
            folded.startsWith(independentFold, foldedIndex) -> independentFold
            // Greek capital sigma has context-sensitive lowercase output (σ or ς).
            codePoint == SIGMA_CODE_POINT && foldedIndex < folded.length -> {
                val foldedCodePoint = folded.codePointAt(foldedIndex)
                if (foldedCodePoint == SMALL_SIGMA_CODE_POINT || foldedCodePoint == FINAL_SIGMA_CODE_POINT) {
                    String(Character.toChars(foldedCodePoint))
                } else {
                    independentFold
                }
            }
            else -> independentFold
        }

        val actualEnd = foldedIndex + actualFold.length
        check(actualEnd <= folded.length && folded.startsWith(actualFold, foldedIndex)) {
            "Unable to map Unicode lowercase output at source offset $sourceIndex"
        }
        for (index in foldedIndex until actualEnd) {
            startToSource[index] = sourceIndex
            endToSourceExclusive[index + 1] = sourceEnd
        }
        foldedIndex = actualEnd
        sourceIndex = sourceEnd
    }

    check(foldedIndex == folded.length) {
        "Unicode lowercase mapping consumed $foldedIndex of ${folded.length} UTF-16 units"
    }
    startToSource[folded.length] = length
    endToSourceExclusive[0] = 0
    return CaseFoldedText(folded, startToSource, endToSourceExclusive)
}

private const val SIGMA_CODE_POINT = 0x03A3
private const val SMALL_SIGMA_CODE_POINT = 0x03C3
private const val FINAL_SIGMA_CODE_POINT = 0x03C2
