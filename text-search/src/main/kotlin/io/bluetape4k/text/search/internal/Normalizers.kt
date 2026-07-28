package io.bluetape4k.text.search.internal

import io.bluetape4k.text.search.NormalizationForm
import io.bluetape4k.text.search.SearchOptions
import java.text.Normalizer
import java.util.Locale

/**
 * 입력 문자열에 [SearchOptions] 정규화 파이프라인을 적용합니다.
 *
 * 적용 순서는 다음과 같습니다.
 * 1. [SearchOptions.normalization]에 따른 유니코드 정규화(NFC 또는 NFKC)
 * 2. [SearchOptions.ignoreCase]가 `true`일 때 [Locale.ROOT] 기준 소문자 변환
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
    return if (opts.ignoreCase) step1.lowercase(Locale.ROOT) else step1
}
