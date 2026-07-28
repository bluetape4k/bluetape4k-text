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
 * 지원하는 모든 언어를 탐지하는 [LanguageDetector]를 생성합니다.
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
 * @param builder [LanguageDetectorBuilder]에 적용할 설정 블록입니다.
 * @return 모든 지원 언어를 대상으로 하는 [LanguageDetector]입니다.
 */
inline fun allLanguageDetector(
    builder: LanguageDetectorBuilder.() -> Unit,
): LanguageDetector =
    LanguageDetectorBuilder
        .fromAllLanguages()
        .apply(builder)
        .build()

/**
 * 지정한 [languages]를 제외한 모든 언어를 탐지하는 [LanguageDetector]를 생성합니다.
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
 * @param languages 탐지 대상에서 제외할 언어 집합입니다.
 * @param builder [LanguageDetectorBuilder]에 적용할 설정 블록입니다.
 * @return 지정 언어를 제외한 모든 지원 언어를 대상으로 하는 [LanguageDetector]입니다.
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
 * 모든 구어 언어를 탐지하는 [LanguageDetector]를 생성합니다.
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
 * @param builder [LanguageDetectorBuilder]에 적용할 설정 블록입니다.
 * @return 모든 구어 언어를 대상으로 하는 [LanguageDetector]입니다.
 */
inline fun allSpokenLanguageDetector(
    builder: LanguageDetectorBuilder.() -> Unit,
): LanguageDetector =
    LanguageDetectorBuilder.fromAllSpokenLanguages().apply(builder).build()

/**
 * 지정한 [languages]만 탐지하는 [LanguageDetector]를 생성합니다.
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
 * @param languages 탐지할 언어 집합입니다.
 * @param builder [LanguageDetectorBuilder]에 적용할 설정 블록입니다.
 * @return 지정 언어만 대상으로 하는 [LanguageDetector]입니다.
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
 * 지정한 [languages]만 탐지하는 [LanguageDetector]를 명시 옵션으로 생성합니다.
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
 * @param languages 탐지할 언어 집합입니다.
 * @param minimumRelativeDistance Lingua가 사용할 최소 상대 거리 임계값입니다.
 * @param isEveryLanguageModelPreloaded 모든 언어 모델을 미리 로드할지 여부입니다.
 * @param isLowAccuracyModeEnabled 저정확도 모드를 사용할지 여부입니다.
 * @return 지정 언어와 옵션으로 구성한 [LanguageDetector]입니다.
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
 * 언어 식별용 ISO 639-1 코드 집합에서 [LanguageDetector]를 생성합니다.
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
 * @param isoCodes 탐지할 언어의 ISO 639-1 코드 집합입니다.
 * @param builder [LanguageDetectorBuilder]에 적용할 설정 블록입니다.
 * @return 지정 ISO 639-1 코드에 해당하는 언어를 대상으로 하는 [LanguageDetector]입니다.
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
 * 언어 식별용 ISO 639-3 코드 집합에서 [LanguageDetector]를 생성합니다.
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
 * @param isoCodes 탐지할 언어의 ISO 639-3 코드 집합입니다.
 * @param builder [LanguageDetectorBuilder]에 적용할 설정 블록입니다.
 * @return 지정 ISO 639-3 코드에 해당하는 언어를 대상으로 하는 [LanguageDetector]입니다.
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
 * 텍스트에 포함된 모든 언어를 탐지해 집합으로 반환합니다.
 *
 * 공백 입력은 빈 집합을 반환합니다. 각 유니코드 문자 토큰은 독립적으로 분류합니다.
 * 짧은 라틴 토큰은 오탐을 줄이기 위해 신뢰도 기반 보정을 거칩니다
 * (예: Lingua가 "Hello"를 SOTHO로 오분류하는 경우). 사용할 수 있는 토큰 결과가 없으면
 * 전체 텍스트 탐지 결과를 단일 원소 집합으로 반환합니다.
 *
 * detector 생성 비용이 크므로 호출 간에 같은 인스턴스를 재사용하는 것이 좋습니다.
 *
 * ```
 * val detector = allLanguageDetector {
 *     withMinimumRelativeDistance(0.0)
 * }
 *
 * detector.detectAllLanguagesOf("Hello 안녕") shouldBeEqualTo setOf(Language.ENGLISH, Language.KOREAN)
 * ```
 *
 * @param text 탐지할 입력 텍스트입니다.
 * @return 입력에서 탐지한 언어 집합입니다. 탐지 결과가 없으면 빈 집합입니다.
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
