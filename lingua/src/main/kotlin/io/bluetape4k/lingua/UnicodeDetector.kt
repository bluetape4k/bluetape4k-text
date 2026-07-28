package io.bluetape4k.lingua

import io.bluetape4k.lingua.UnicodeDetector.Companion.SupportedLanguages
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.trace
import java.util.*

/**
 * 유니코드 블록 규칙으로 문자열에 특정 locale의 문자가 포함되는지 판정합니다.
 *
 * [SupportedLanguages]에 포함된 locale만 인식합니다. 아스키 문자는 모든 locale에서 통과합니다.
 * 필터링은 입력 문자열을 변경하지 않고 항상 새 배열을 반환합니다.
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
     * [text]에서 [locale]에 속하는 문자만 골라 [CharArray]로 반환합니다.
     *
     * @param text 필터링할 입력 문자열입니다.
     * @param locale 유지할 문자 범위를 결정하는 locale입니다.
     * @return [locale]에 속하거나 ASCII인 문자 배열입니다.
     */
    fun filterString(text: String, locale: Locale): CharArray {
        log.debug { "언어[${locale.language}] 문자만 필터링합니다" }
        return text.mapNotNull { filterChar(it, locale) }.toCharArray()
    }

    /**
     * [char]가 [locale]에 속하면 그대로 반환하고, 아니면 `null`을 반환합니다.
     *
     * 아스키 문자는 항상 반환합니다. 지원하지 않는 locale은 항상 `null`을 반환합니다.
     *
     * @param char 검사할 문자입니다.
     * @param locale 문자 범위를 결정하는 locale입니다.
     * @return [char]가 locale 범위에 속하거나 ASCII이면 [char], 아니면 `null`입니다.
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
        log.trace { "char=$char, language=${locale.language}, c=$c" }

        return c
    }

    /** [text]에 [locale]에 속하는 문자가 하나 이상 있으면 `true`를 반환합니다. */
    fun containsAny(text: String, locale: Locale): Boolean {
        return filterString(text, locale).isNotEmpty()
    }

    /** [text]의 모든 문자가 [locale]에 속하면 `true`를 반환합니다. */
    fun containsAll(text: String, locale: Locale): Boolean {
        return filterString(text, locale).size == text.length
    }
}
