package io.bluetape4k.text.search

import java.io.Serializable

/**
 * Word boundary detection mode for Aho-Corasick matching.
 *
 * - [NONE]: substring matches are allowed (no boundary enforcement)
 * - [LATIN_ALPHA]: matches only at [Character.isAlphabetic] boundaries; prefer [WHITESPACE_SEPARATED] for Korean/CJK text
 * - [WHITESPACE_SEPARATED]: matches only at whitespace boundaries
 */
enum class WordBoundary {
    /** No boundary — substring matches are allowed. */
    NONE,

    /**
     * Alphabetic boundary based on [Character.isAlphabetic].
     * **Note**: [Character.isAlphabetic] includes CJK ideographs and Hangul; use [WHITESPACE_SEPARATED] for Korean text.
     */
    LATIN_ALPHA,

    /** Whitespace boundary — only matches tokens delimited by [Character.isWhitespace]. */
    WHITESPACE_SEPARATED,
}

/**
 * Unicode normalization form applied to both keywords and search text before matching.
 *
 * - [NONE]: no normalization (default; suitable for ASCII-only text)
 * - [NFC]: Canonical Decomposition followed by Canonical Composition (useful for Hangul jamo composition)
 * - [NFKC]: Compatibility Decomposition followed by Canonical Composition (normalizes fullwidth chars, ligatures, etc.)
 */
enum class NormalizationForm {
    /** No normalization. */
    NONE,

    /**
     * NFC (Canonical Decomposition + Canonical Composition).
     * Example: decomposed jamo `ㄴㅏ` → composed `나`.
     */
    NFC,

    /**
     * NFKC (Compatibility Decomposition + Canonical Composition).
     * Example: `㈜` → `(주)` — length may change; handled automatically by [io.bluetape4k.text.search.internal.OffsetMapping].
     */
    NFKC,
}

/**
 * Search options for [AhoCorasickAutomaton]. All fields are immutable; use [copy] to derive variants.
 *
 * ```kotlin
 * val opts = SearchOptions(ignoreCase = true, wordBoundary = WordBoundary.WHITESPACE_SEPARATED)
 * val opts2 = opts.copy(allowOverlaps = false)
 * ```
 *
 * **Note**: [stopOnFirstMatch] is ignored by [io.bluetape4k.text.search.flow.matchesAsFlow].
 * Use `take(1)` on the Flow to stop after the first match.
 */
data class SearchOptions(
    /** Whether to ignore case during matching (default: `false`). */
    val ignoreCase: Boolean = false,

    /** Whether overlapping matches are allowed (default: `true`). When `false`, longer keywords take priority. */
    val allowOverlaps: Boolean = true,

    /** Word boundary detection mode (default: [WordBoundary.NONE]). */
    val wordBoundary: WordBoundary = WordBoundary.NONE,

    /** Unicode normalization form (default: [NormalizationForm.NONE]). */
    val normalization: NormalizationForm = NormalizationForm.NONE,

    /**
     * Whether to stop after the first match is found (default: `false`).
     * **Note**: ignored by [io.bluetape4k.text.search.flow.matchesAsFlow].
     */
    val stopOnFirstMatch: Boolean = false,
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}
