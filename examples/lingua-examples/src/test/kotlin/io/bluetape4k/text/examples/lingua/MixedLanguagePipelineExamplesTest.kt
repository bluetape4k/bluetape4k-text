package io.bluetape4k.text.examples.lingua

import com.github.pemistahl.lingua.api.Language
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldContain
import org.junit.jupiter.api.Test

class MixedLanguagePipelineExamplesTest {

    @Test
    fun `언어 구간을 감지하고 한국어와 일본어 facade로 라우팅한다`() {
        val result = runMixedLanguagePipeline("Hello 안녕하세요 こんにちは")

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
        val result = runMixedLanguagePipeline("안녕 こんにちは")
        val output = renderMixedLanguagePipeline(result)

        output shouldContain "KOREAN 0..2"
        output shouldContain "JAPANESE 3..8"
        output shouldContain "tokens[KOREAN]"
    }
}
