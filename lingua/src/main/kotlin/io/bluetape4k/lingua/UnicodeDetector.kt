package io.bluetape4k.lingua

import io.bluetape4k.lingua.UnicodeDetector.Companion.SupportedLanguages
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.trace
import java.util.*

/**
 * Determines whether a string contains characters from a given locale using Unicode block rules.
 *
 * Only locales listed in [SupportedLanguages] are recognized; ASCII characters pass all locales.
 * Filtering always returns a new array without mutating the input string.
 *
 * ```kotlin
 * val detector = UnicodeDetector()
 * val chars = detector.filterString("안녕하세요 hello", Locale.KOREAN)
 * // chars.isNotEmpty() == true
 * ```
 */
class UnicodeDetector {

    companion object: KLogging() {
        val SupportedLanguages: List<Locale> = listOf(
            Locale.KOREAN,
            Locale.JAPANESE,
            Locale.ENGLISH,
            Locale.CHINESE,
            Locale.of("th")
        )
    }

    /**
     * Filters [text] to only the characters that belong to [locale], returning them as a [CharArray].
     */
    fun filterString(text: String, locale: Locale): CharArray {
        log.debug { "filter language[${locale.language}] chars..." }
        return text.mapNotNull { filterChar(it, locale) }.toCharArray()
    }

    /**
     * Returns [char] if it belongs to [locale], or `null` otherwise.
     *
     * ASCII characters are always returned. Unsupported locales always return `null`.
     */
    fun filterChar(char: Char, locale: Locale): Char? {
        if (char.isAscii) {
            return char
        }

        if (locale !in SupportedLanguages) {
            return null
        }

        val c = when (locale.language) {
            "ko" -> if (char.isKorean) char else null
            "ja" -> if (char.isJapanese) char else null
            "en" -> if (char.isAscii) char else null
            "zh" -> if (char.isChinese) char else null
            "th" -> if (char.isThai) char else null
            else -> null
        }
        log.trace { "char=$char, languge=${locale.language}, c=$c" }

        return c
    }

    /** Returns `true` if [text] contains at least one character belonging to [locale]. */
    fun containsAny(text: String, locale: Locale): Boolean {
        return filterString(text, locale).isNotEmpty()
    }

    /** Returns `true` if every character in [text] belongs to [locale]. */
    fun containsAll(text: String, locale: Locale): Boolean {
        return filterString(text, locale).size == text.length
    }
}
