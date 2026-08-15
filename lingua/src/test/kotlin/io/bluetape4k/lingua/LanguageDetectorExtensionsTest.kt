package io.bluetape4k.lingua

import com.github.pemistahl.lingua.api.Language
import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeTrue
import org.junit.jupiter.api.Test

class LanguageDetectorExtensionsTest: AbstractLinguaTest() {

    private val detector = allLanguageDetector {
        withMinimumRelativeDistance(0.0)
    }
    private val mixedText = "Parlez-vous français? Ich spreche nur ein bisschen Deutsch. A little bit is better than nothing."

    @Test
    fun `blank 입력이면 빈 집합을 반환한다`() {
        detector.detectAllLanguagesOf("   ") shouldBeEqualTo emptySet()
    }

    @Test
    fun `단일 언어면 singleton set을 반환한다`() {
        detector.detectAllLanguagesOf("Hello world") shouldBeEqualTo setOf(Language.ENGLISH)
    }

    @Test
    fun `혼합 언어면 모든 검출 언어를 집합으로 반환한다`() {
        detector.detectAllLanguagesOf(mixedText) shouldBeEqualTo setOf(
            Language.FRENCH,
            Language.GERMAN,
            Language.ENGLISH,
        )
    }

    @Test
    fun `영어와 한국어가 섞이면 모든 검출 언어를 집합으로 반환한다`() {
        detector.detectAllLanguagesOf("Hello 안녕") shouldBeEqualTo setOf(
            Language.ENGLISH,
            Language.KOREAN,
        )
    }

    @Test
    fun `세 개 언어가 섞이면 모든 검출 언어를 집합으로 반환한다`() {
        detector.detectAllLanguagesOf("Hello 안녕 こんにちは") shouldBeEqualTo setOf(
            Language.ENGLISH,
            Language.KOREAN,
            Language.JAPANESE,
        )
    }

    @Test
    fun `인식할 수 없는 입력이면 빈 집합을 반환한다`() {
        detector.detectAllLanguagesOf("🔥🎉🧪") shouldBeEqualTo emptySet()
    }

    @Test
    fun `혼합 언어의 UTF-16 구간과 신뢰도를 반환한다`() {
        val text = "Hello 안녕하세요 こんにちは"

        val segments = detector.detectLanguageSegments(text)

        segments.map { text.substring(it.start, it.endExclusive) } shouldBeEqualTo listOf(
            "Hello",
            "안녕하세요",
            "こんにちは",
        )
        segments.map { it.language } shouldBeEqualTo listOf(
            Language.ENGLISH,
            Language.KOREAN,
            Language.JAPANESE,
        )
        segments.forEach { (it.confidence in 0.55..1.0).shouldBeTrue() }
    }

    @Test
    fun `빈 입력이면 언어 구간을 반환하지 않는다`() {
        detector.detectLanguageSegments("") shouldBeEqualTo emptyList()
    }

    @Test
    fun `구두점과 이모지는 언어 구간에 포함하지 않는다`() {
        val text = "Hello, 안녕! 🔥"

        val segments = detector.detectLanguageSegments(text)

        segments.map { text.substring(it.start, it.endExclusive) } shouldBeEqualTo listOf("Hello", "안녕")
        segments[0].endExclusive shouldBeEqualTo 5
        segments[1].start shouldBeEqualTo 7
    }

    @Test
    fun `신뢰도 임계값으로 짧은 모델 토큰을 걸러낸다`() {
        detector.detectLanguageSegments("x", minimumConfidence = 0.99) shouldBeEqualTo emptyList()
    }

    @Test
    fun `알 수 없는 토큰은 fallback에서 빈 집합이 된다`() {
        detector.detectAllLanguagesOf("ᚠᚢᚦ") shouldBeEqualTo emptySet()
    }

    @Test
    fun `신뢰도 0과 1 경계가 각각 unknown과 확정 구간을 보존한다`() {
        val unknownAtZero = detector.detectLanguageSegments("ᚠᚢᚦ", minimumConfidence = 0.0)
        unknownAtZero shouldBeEqualTo listOf(
            LanguageSegment(0, 3, Language.UNKNOWN, 0.0),
        )
        detector.detectLanguageSegments("ᚠᚢᚦ", minimumConfidence = 1.0) shouldBeEqualTo emptyList()

        detector.detectLanguageSegments("안녕", minimumConfidence = 1.0) shouldBeEqualTo listOf(
            LanguageSegment(0, 2, Language.KOREAN, 1.0),
        )
    }

    @Test
    fun `유효하지 않은 신뢰도 임계값은 거부한다`() {
        assertFailsWith<IllegalArgumentException> {
            detector.detectLanguageSegments("Hello", minimumConfidence = 1.1)
        }
    }

    @Test
    fun `인접한 같은 언어 문자는 하나의 구간으로 합친다`() {
        val text = "안녕가나다"

        detector.detectLanguageSegments(text) shouldBeEqualTo listOf(
            LanguageSegment(0, text.length, Language.KOREAN, 1.0),
        )
    }

    @Test
    fun `보조 평면 이모지 뒤에서도 UTF-16 구간 offset을 유지한다`() {
        val text = "🔥Hello 안녕"
        val segments = detector.detectLanguageSegments(text)

        segments.map { text.substring(it.start, it.endExclusive) } shouldBeEqualTo listOf("Hello", "안녕")
        segments.map { it.start to it.endExclusive } shouldBeEqualTo listOf(2 to 7, 8 to 10)
    }


    @Test
    fun latinPhrasesUsePreferredLatinCandidates() {
        detector.detectAllLanguagesOf("Hola servicio. Bonjour utilisateurs! Grazie mille") shouldBeEqualTo setOf(
            Language.SPANISH,
            Language.FRENCH,
            Language.ITALIAN,
        )
    }

    @Test
    fun singleLetterLatinTokenIsIgnoredInMixedText() {
        detector.detectAllLanguagesOf("A 안녕하세요") shouldBeEqualTo setOf(Language.KOREAN)
    }

    @Test
    fun longNonPreferredLatinTokenIsIgnoredInMixedText() {
        detector.detectAllLanguagesOf("Xylophonemuseum 안녕하세요") shouldBeEqualTo setOf(Language.KOREAN)
    }

}
