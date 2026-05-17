package io.bluetape4k.text.search

import java.io.Serializable

/**
 * A token produced by [io.bluetape4k.text.search.AhoCorasickAutomaton.tokenize].
 *
 * The input text is split into keyword-matching spans ([Match]) and non-matching spans ([Fragment]).
 *
 * @param V covariant type of the value associated with matched keywords
 */
sealed interface SearchToken<out V> : Serializable {

    /**
     * A keyword-matching span.
     *
     * @param text the matched substring extracted from the original text
     * @param match detailed match information ([AhoCorasickMatch])
     */
    data class Match<out V>(
        val text: String,
        val match: AhoCorasickMatch<V>,
    ) : SearchToken<V> {
        companion object {
            private const val serialVersionUID: Long = 1L
        }
    }

    /**
     * A non-matching span (plain text between keyword matches).
     *
     * @param text the non-matching substring extracted from the original text
     */
    data class Fragment(
        val text: String,
    ) : SearchToken<Nothing> {
        companion object {
            private const val serialVersionUID: Long = 1L
        }
    }
}
