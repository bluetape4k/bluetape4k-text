package io.bluetape4k.text.search

import java.io.Serializable

/**
 * A single keyword match result produced by [AhoCorasickAutomaton].
 *
 * Named `AhoCorasickMatch` to avoid collision with `kotlin.text.MatchResult`.
 *
 * @param V covariant type of the value associated with the matched keyword
 * @param start inclusive start offset in the original text
 * @param end inclusive end offset in the original text
 * @param keyword matched keyword (in normalized form when normalization is active)
 * @param value value associated with the keyword
 */
data class AhoCorasickMatch<out V>(
    val start: Int,
    val end: Int,
    val keyword: String,
    val value: V,
) : Serializable {

    companion object {
        private const val serialVersionUID: Long = 1L
    }

    /** Match length (`end - start + 1`). */
    val length: Int get() = end - start + 1
}
