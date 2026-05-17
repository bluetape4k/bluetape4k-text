package io.bluetape4k.text.search.internal

import io.bluetape4k.text.search.NormalizationForm
import io.bluetape4k.text.search.SearchOptions
import java.text.Normalizer
import java.util.Locale

/**
 * Applies the [SearchOptions] normalization pipeline to the input string.
 *
 * Steps applied in order:
 * 1. Unicode normalization (NFC or NFKC) per [SearchOptions.normalization]
 * 2. Lowercase conversion with [Locale.ROOT] when [SearchOptions.ignoreCase] is `true`
 *
 * Must be applied identically at keyword-registration time and search time to guarantee consistent matching.
 *
 * ```kotlin
 * val opts = SearchOptions(ignoreCase = true, normalization = NormalizationForm.NFC)
 * val normalized = applyPipeline("APPLE", opts) // "apple"
 * ```
 *
 * @param s input string to normalize
 * @param opts search options defining the normalization pipeline
 * @return normalized and optionally lowercased string
 */
internal fun applyPipeline(s: CharSequence, opts: SearchOptions): String {
    val step1 = when (opts.normalization) {
        NormalizationForm.NONE -> s.toString()
        NormalizationForm.NFC -> Normalizer.normalize(s, Normalizer.Form.NFC)
        NormalizationForm.NFKC -> Normalizer.normalize(s, Normalizer.Form.NFKC)
    }
    return if (opts.ignoreCase) step1.lowercase(Locale.ROOT) else step1
}
