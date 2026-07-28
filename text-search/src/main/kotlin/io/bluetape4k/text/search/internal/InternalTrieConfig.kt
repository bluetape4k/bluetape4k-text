package io.bluetape4k.text.search.internal

import io.bluetape4k.logging.KLogging
import java.io.Serializable

/**
 * 내부 Aho-Corasick trie 설정입니다.
 *
 * ```kotlin
 * val config = InternalTrieConfig.builder().ignoreCase(true).onlyWholeWords(true).build()
 * // config.ignoreCase == true
 * ```
 *
 * @property allowOverlaps 겹치는 match를 허용할지 여부입니다.
 * @property onlyWholeWords whole word만 match할지 여부입니다. 알파벳 경계를 사용합니다.
 * @property onlyWholeWordsWhiteSpaceSeparated whole word만 match할지 여부입니다. 공백 경계를 사용합니다.
 * @property ignoreCase 대소문자를 무시할지 여부입니다.
 * @property stopOnHit 첫 match에서 중단할지 여부입니다.
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

        /** [allowOverlaps]를 설정합니다. */
        fun allowOverlaps(value: Boolean = true) = apply {
            this.allowOverlaps = value
        }

        /** [onlyWholeWords]를 설정합니다. */
        fun onlyWholeWords(value: Boolean = false) = apply {
            this.onlyWholeWords = value
        }

        /** [onlyWholeWordsWhiteSpaceSeparated]를 설정합니다. */
        fun onlyWholeWordsWhiteSpaceSeparated(value: Boolean = false) = apply {
            this.onlyWholeWordsWhiteSpaceSeparated = value
        }

        /** [ignoreCase]를 설정합니다. */
        fun ignoreCase(value: Boolean = false) = apply {
            this.ignoreCase = value
        }

        /** [stopOnHit]을 설정합니다. */
        fun stopOnHit(value: Boolean = false) = apply {
            this.stopOnHit = value
        }

        /** 현재 설정으로 [InternalTrieConfig]를 생성합니다. */
        fun build(): InternalTrieConfig {
            return InternalTrieConfig(allowOverlaps, onlyWholeWords, onlyWholeWordsWhiteSpaceSeparated, ignoreCase, stopOnHit)
        }
    }
}
