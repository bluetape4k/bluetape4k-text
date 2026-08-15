package io.bluetape4k.text.examples.lingua

import com.github.pemistahl.lingua.api.Language
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldContain
import org.junit.jupiter.api.Test

class MixedLanguagePipelineExamplesTest {

    @Test
    fun `언어 구간을 감지하고 한국어와 일본어 facade로 라우팅한다`() {
        val detector = createMixedLanguageDetector()
        val result = runMixedLanguagePipeline("Hello 안녕하세요 こんにちは", detector)

        result.segments.map { it.language } shouldBeEqualTo listOf(
            Language.ENGLISH,
            Language.KOREAN,
            Language.JAPANESE,
        )
        result.tokenCounts[Language.KOREAN] shouldBeEqualTo 1
        result.tokenCounts[Language.JAPANESE] shouldBeEqualTo 1
    }

    @Test
    fun `원문 offset과 라우팅 결과를 출력한다`() {
        val detector = createMixedLanguageDetector()
        val result = runMixedLanguagePipeline("안녕 こんにちは", detector)
        val output = renderMixedLanguagePipeline(result)

        output shouldContain "KOREAN 0..2"
        output shouldContain "JAPANESE 3..8"
        output shouldContain "tokens[KOREAN]"
    }

    @Test
    fun `하나의 detector를 여러 입력에서 재사용해 결과를 유지한다`() {
        val detector = createMixedLanguageDetector()

        val first = runMixedLanguagePipeline("Hello 안녕하세요 こんにちは", detector)
        val second = runMixedLanguagePipeline("안녕 こんにちは", detector)

        first.segments.map { it.language } shouldBeEqualTo listOf(
            Language.ENGLISH,
            Language.KOREAN,
            Language.JAPANESE,
        )
        second.segments.map { it.language } shouldBeEqualTo listOf(
            Language.KOREAN,
            Language.JAPANESE,
        )
        second.tokenCounts[Language.KOREAN] shouldBeEqualTo 1
        second.tokenCounts[Language.JAPANESE] shouldBeEqualTo 1
    }

    @Test
    fun `preload와 lazy detector가 같은 pipeline 결과를 반환한다`() {
        val text = "Hello 안녕하세요 こんにちは"

        val preloaded = runMixedLanguagePipeline(text, createMixedLanguageDetector(preloadModels = true))
        val lazy = runMixedLanguagePipeline(text, createMixedLanguageDetector(preloadModels = false))

        lazy.segments shouldBeEqualTo preloaded.segments
        lazy.tokenCounts shouldBeEqualTo preloaded.tokenCounts
    }
}
