package io.bluetape4k.text.search.internal

import io.bluetape4k.logging.KLogging
import java.io.Serializable

/**
 * Configuration for the internal Aho-Corasick trie.
 *
 * ```kotlin
 * val config = InternalTrieConfig.builder().ignoreCase(true).onlyWholeWords(true).build()
 * // config.ignoreCase == true
 * ```
 *
 * @property allowOverlaps whether overlapping matches are allowed
 * @property onlyWholeWords whether to match whole words only (alphabetic boundary)
 * @property onlyWholeWordsWhiteSpaceSeparated whether to match whole words only (whitespace boundary)
 * @property ignoreCase whether to ignore case
 * @property stopOnHit whether to stop on the first match
 */
internal data class InternalTrieConfig(
    var allowOverlaps: Boolean = true,
    var onlyWholeWords: Boolean = false,
    var onlyWholeWordsWhiteSpaceSeparated: Boolean = false,
    var ignoreCase: Boolean = false,
    var stopOnHit: Boolean = false,
): Serializable {

    companion object : KLogging() {
        private const val serialVersionUID = 1L

        @JvmStatic
        val DEFAULT = InternalTrieConfig()

        @JvmStatic
        fun builder(): Builder = Builder()
    }

    class Builder {
        private var allowOverlaps: Boolean = true
        private var onlyWholeWords: Boolean = false
        private var onlyWholeWordsWhiteSpaceSeparated: Boolean = false
        private var ignoreCase: Boolean = false
        private var stopOnHit: Boolean = false

        /** Sets [allowOverlaps]. */
        fun allowOverlaps(value: Boolean = true) = apply {
            this.allowOverlaps = value
        }

        /** Sets [onlyWholeWords]. */
        fun onlyWholeWords(value: Boolean = false) = apply {
            this.onlyWholeWords = value
        }

        /** Sets [onlyWholeWordsWhiteSpaceSeparated]. */
        fun onlyWholeWordsWhiteSpaceSeparated(value: Boolean = false) = apply {
            this.onlyWholeWordsWhiteSpaceSeparated = value
        }

        /** Sets [ignoreCase]. */
        fun ignoreCase(value: Boolean = false) = apply {
            this.ignoreCase = value
        }

        /** Sets [stopOnHit]. */
        fun stopOnHit(value: Boolean = false) = apply {
            this.stopOnHit = value
        }

        /** Builds an [InternalTrieConfig] from the current settings. */
        fun build(): InternalTrieConfig {
            return InternalTrieConfig(allowOverlaps, onlyWholeWords, onlyWholeWordsWhiteSpaceSeparated, ignoreCase, stopOnHit)
        }
    }
}
