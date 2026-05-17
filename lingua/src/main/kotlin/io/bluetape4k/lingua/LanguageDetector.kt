package io.bluetape4k.lingua

import com.github.pemistahl.lingua.api.IsoCode639_1
import com.github.pemistahl.lingua.api.IsoCode639_3
import com.github.pemistahl.lingua.api.Language
import com.github.pemistahl.lingua.api.LanguageDetector
import com.github.pemistahl.lingua.api.LanguageDetectorBuilder

private val mixedLanguageTokenRegex = Regex("\\p{L}+(?:['’-]\\p{L}+)*")
private val mixedLanguagePhraseDelimiterRegex = Regex("[.!?\n]+")

private val preferredLatinLanguages: Set<Language> = setOf(
    Language.ENGLISH,
    Language.SPANISH,
    Language.FRENCH,
    Language.GERMAN,
    Language.PORTUGUESE,
    Language.ITALIAN,
    Language.DUTCH,
)

private const val preferredLatinCandidateScanDepth = 10
private const val shortLatinTokenLength = 5
private const val preferredLatinConfidenceThreshold = 0.80


/**
 * Creates a [LanguageDetector] that detects all supported languages.
 *
 * ```
 * val detector = allLanguageDetector {
 *      withPreloadedLanguageModels()
 *      withMinimumRelativeDistance(0.0)
 * }
 *
 * detector.detectLanguageOf("Hello, World") shouldBeEqualTo Language.ENGLISH
 * detector.detectLanguageOf("안녕하세요.") shouldBeEqualTo Language.KOREAN
 * ```
 *
 * @param builder configuration block applied to [LanguageDetectorBuilder]
 */
inline fun allLanguageDetector(
    builder: LanguageDetectorBuilder.() -> Unit,
): LanguageDetector =
    LanguageDetectorBuilder
        .fromAllLanguages()
        .apply(builder)
        .build()

/**
 * Creates a [LanguageDetector] covering all languages except the specified [languages].
 *
 * ```
 * val exceptLanguages = setOf(Language.GERMAN, Language.THAI)
 * val detector = allLanguageWithoutDetector(exceptLanguages) {
 *    withPreloadedLanguageModels()
 *    withMinimumRelativeDistance(0.0)
 * }
 *
 * detector.detectLanguageOf("Hello, World") shouldBeEqualTo Language.ENGLISH
 * detector.detectLanguageOf("안녕하세요.") shouldBeEqualTo Language.KOREAN
 * ```
 *
 * @param languages languages to exclude
 * @param builder configuration block applied to [LanguageDetectorBuilder]
 */
inline fun allLanguageWithoutDetector(
    languages: Set<Language>,
    builder: LanguageDetectorBuilder.() -> Unit,
): LanguageDetector =
    LanguageDetectorBuilder
        .fromAllLanguagesWithout(*languages.toTypedArray())
        .apply(builder)
        .build()


/**
 * Creates a [LanguageDetector] covering all spoken languages.
 *
 * ```
 * val detector = allSpokenLanguageDetector {
 *     withPreloadedLanguageModels()
 *     withMinimumRelativeDistance(0.0)
 *     withLowAccuracyMode()
 * }
 *
 * detector.detectLanguageOf("Hello, World") shouldBeEqualTo Language.ENGLISH
 * detector.detectLanguageOf("안녕하세요.") shouldBeEqualTo Language.KOREAN
 * ```
 *
 * @param builder configuration block applied to [LanguageDetectorBuilder]
 */
inline fun allSpokenLanguageDetector(
    builder: LanguageDetectorBuilder.() -> Unit,
): LanguageDetector =
    LanguageDetectorBuilder.fromAllSpokenLanguages().apply(builder).build()

/**
 * Creates a [LanguageDetector] limited to the specified [languages].
 *
 * ```
 * val languages = setOf(Language.ENGLISH, Language.KOREAN)
 * val detector = languageDetectorOf(languages) {
 *    withPreloadedLanguageModels()
 *    withMinimumRelativeDistance(0.0)
 *    withLowAccuracyMode()
 * }
 *
 * detector.detectLanguageOf("Hello, World") shouldBeEqualTo Language.ENGLISH
 * detector.detectLanguageOf("안녕하세요.") shouldBeEqualTo Language.KOREAN
 * ```
 *
 * @param languages languages to detect
 * @param builder configuration block applied to [LanguageDetectorBuilder]
 */
@JvmName("languageDetectorOfLanguage")
inline fun languageDetectorOf(
    languages: Set<Language>,
    builder: LanguageDetectorBuilder.() -> Unit,
): LanguageDetector =
    LanguageDetectorBuilder
        .fromLanguages(*languages.toTypedArray())
        .apply(builder)
        .build()

/**
 * Creates a [LanguageDetector] limited to the specified [languages] with explicit options.
 *
 * ```
 * val languages = setOf(Language.ENGLISH, Language.KOREAN)
 * val detector = languageDetectorOf(
 *      languages,
 *      minimumRelativeDistance = 0.0,
 *      isEveryLanguageModelPreloaded = true,
 *      isLowAccuracyModeEnabled = false
 * )
 *
 * detector.detectLanguageOf("Hello, World") shouldBeEqualTo Language.ENGLISH
 * detector.detectLanguageOf("안녕하세요.") shouldBeEqualTo Language.KOREAN
 * ```
 *
 * @param languages languages to detect
 * @param minimumRelativeDistance minimum relative distance threshold
 * @param isEveryLanguageModelPreloaded whether to preload all language models
 * @param isLowAccuracyModeEnabled whether to enable low-accuracy mode
 */
fun languageDetectorOf(
    languages: Set<Language> = Language.all().toSet(),
    minimumRelativeDistance: Double = 0.0,
    isEveryLanguageModelPreloaded: Boolean = true,
    isLowAccuracyModeEnabled: Boolean = false,
): LanguageDetector =
    languageDetectorOf(languages) {
        withMinimumRelativeDistance(minimumRelativeDistance)
        if (isEveryLanguageModelPreloaded) {
            withPreloadedLanguageModels()
        }
        if (isLowAccuracyModeEnabled) {
            withLowAccuracyMode()
        }
    }

/**
 * Creates a [LanguageDetector] from a set of ISO 639-1 codes.
 *
 * ```
 * val isoCodes = setOf(IsoCode639_1.EN, IsoCode639_1.KO)
 * val detector = languageDetectorOf(isoCodes) {
 *    withPreloadedLanguageModels()
 *    withMinimumRelativeDistance(0.0)
 *    withLowAccuracyMode()
 * }
 *
 * detector.detectLanguageOf("Hello, World") shouldBeEqualTo Language.ENGLISH
 * detector.detectLanguageOf("안녕하세요.") shouldBeEqualTo Language.KOREAN
 * ```
 *
 * @param isoCodes ISO 639-1 codes for the languages to detect
 * @param builder configuration block applied to [LanguageDetectorBuilder]
 */
@JvmName("languageDetectorOfIsoCode639_1")
inline fun languageDetectorOf(
    isoCodes: Set<IsoCode639_1>,
    builder: LanguageDetectorBuilder.() -> Unit,
): LanguageDetector =
    LanguageDetectorBuilder
        .fromIsoCodes639_1(*isoCodes.toTypedArray())
        .apply(builder)
        .build()

/**
 * Creates a [LanguageDetector] from a set of ISO 639-3 codes.
 *
 * ```
 * val isoCodes = setOf(IsoCode639_3.EN, IsoCode639_3.KO)
 * val detector = languageDetectorOf(isoCodes) {
 *    withPreloadedLanguageModels()
 *    withMinimumRelativeDistance(0.0)
 *    withLowAccuracyMode()
 * }
 *
 * detector.detectLanguageOf("Hello, World") shouldBeEqualTo Language.ENGLISH
 * detector.detectLanguageOf("안녕하세요.") shouldBeEqualTo Language.KOREAN
 * ```
 *
 * @param isoCodes ISO 639-3 codes for the languages to detect
 * @param builder configuration block applied to [LanguageDetectorBuilder]
 */
@JvmName("languageDetectorOfIsoCode639_3")
inline fun languageDetectorOf(
    isoCodes: Set<IsoCode639_3>,
    builder: LanguageDetectorBuilder.() -> Unit,
): LanguageDetector =
    LanguageDetectorBuilder
        .fromIsoCodes639_3(*isoCodes.toTypedArray())
        .apply(builder)
        .build()

/**
 * Detects all languages present in the text and returns them as a set.
 *
 * Blank input returns an empty set. Each Unicode-letter token is classified independently;
 * short Latin tokens are subject to confidence-based correction to avoid false positives
 * (e.g. Lingua mis-classifying "Hello" as SOTHO). If no usable token results are found,
 * the full-text detection result is returned as a singleton set.
 *
 * Reuse the detector instance across calls — construction is expensive.
 *
 * ```
 * val detector = allLanguageDetector {
 *     withMinimumRelativeDistance(0.0)
 * }
 *
 * detector.detectAllLanguagesOf("Hello 안녕") shouldBeEqualTo setOf(Language.ENGLISH, Language.KOREAN)
 * ```
 */
fun LanguageDetector.detectAllLanguagesOf(text: String): Set<Language> {
    if (text.isBlank()) {
        return emptySet()
    }

    val detected = linkedSetOf<Language>()

    mixedLanguagePhraseDelimiterRegex
        .split(text)
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .forEach { phrase ->
            if (phrase.isLatinPhrase()) {
                detectLanguageOfLatinPhrase(phrase)?.let { detected += it }
            } else {
                mixedLanguageTokenRegex
                    .findAll(phrase)
                    .map { it.value }
                    .mapNotNull { token -> detectLanguageOfToken(token) }
                    .forEach { detected += it }
            }
        }

    if (detected.isNotEmpty()) {
        return detected
    }

    return detectLanguageOf(text)
        .takeIf { it != Language.UNKNOWN }
        ?.let { setOf(it) }
        ?: emptySet()
}

private fun LanguageDetector.detectLanguageOfLatinPhrase(phrase: String): Language? {
    val detected = detectLanguageOf(phrase)
    if (detected in preferredLatinLanguages) {
        return detected
    }

    return computeLanguageConfidenceValues(phrase).entries.asSequence()
        .take(preferredLatinCandidateScanDepth)
        .firstOrNull { it.key in preferredLatinLanguages && it.value >= preferredLatinConfidenceThreshold }
        ?.key
        ?: detected.takeIf { it != Language.UNKNOWN }
}

private fun LanguageDetector.detectLanguageOfToken(token: String): Language? {
    val detected = detectLanguageOf(token)
    if (detected == Language.UNKNOWN) {
        return null
    }
    if (!token.isLatinToken()) {
        return detected
    }
    if (token.length == 1) {
        return null
    }
    if (!token.isShortLatinToken()) {
        return detected.takeIf { it in preferredLatinLanguages }
    }
    if (detected in preferredLatinLanguages) {
        return detected
    }

    val candidates = computeLanguageConfidenceValues(token).entries
        .take(preferredLatinCandidateScanDepth)
        .toList()
    val englishCandidate = candidates.firstOrNull { it.key == Language.ENGLISH }
    if (token.isAsciiWord() && englishCandidate != null && englishCandidate.value >= preferredLatinConfidenceThreshold) {
        return Language.ENGLISH
    }

    return candidates.firstOrNull { it.key in preferredLatinLanguages }?.key
}

private fun String.isShortLatinToken(): Boolean =
    length <= shortLatinTokenLength && isLatinToken()

private fun String.isLatinToken(): Boolean = all { it.isLatin }

private fun String.isLatinPhrase(): Boolean =
    filterNot(Char::isWhitespace).all { it.isLatin || it == '\'' || it == '’' || it == '-' }

private fun String.isAsciiWord(): Boolean = all(Char::isAscii)
