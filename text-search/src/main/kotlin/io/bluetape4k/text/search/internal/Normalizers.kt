package io.bluetape4k.text.search.internal

import io.bluetape4k.text.search.NormalizationForm
import io.bluetape4k.text.search.SearchOptions
import java.text.Normalizer

/**
 * 입력 문자열에 [SearchOptions] 정규화 파이프라인을 적용합니다.
 *
 * 적용 순서는 다음과 같습니다.
 * 1. [SearchOptions.normalization]에 따른 유니코드 정규화(NFC 또는 NFKC)
 * 2. [SearchOptions.ignoreCase]가 `true`일 때 UTF-16 문자 단위 소문자 변환
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
 * 검색 keyword와 text에 동일하게 적용하는 offset 보존형 소문자 변환입니다.
 *
 * [String.lowercase]는 `İ`나 그리스어 sigma처럼 길이 또는 문맥에 따라 결과가 달라질 수 있어
 * 정규화된 text의 offset과 keyword 등록 결과가 어긋날 수 있습니다. 문자 단위 변환은 UTF-16
 * 길이를 보존해 [OffsetMapping]의 원본 위치 계약을 유지합니다.
 */
internal fun String.lowercaseCharByChar(): String = buildString(length) {
    for (c in this@lowercaseCharByChar) append(c.lowercaseChar())
}
