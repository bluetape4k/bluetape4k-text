package io.bluetape4k.text.examples.search

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldContain
import org.junit.jupiter.api.Test

class StreamingSearchExamplesTest {

    @Test
    fun `청크 경계를 가로지르는 scanner 결과가 eager 결과와 같다`() {
        val report = runStreamingSearchExample(
            "password reset before card declined and secret",
            chunkSize = 5,
        )

        report.equivalent.shouldBeTrue()
        report.streamed.map { it.value } shouldBeEqualTo listOf(
            "ACCOUNT_TAKEOVER",
            "PAYMENT_RISK",
            "SECRET_DATA",
        )
    }

    @Test
    fun `보고서에 global offset과 동등성 결과를 기록한다`() {
        val output = renderStreamingSearchExample(
            runStreamingSearchExample("secret", chunkSize = 2)
        )

        output shouldContain "equivalent=true"
        output shouldContain "secret 0..5"
    }

    @Test
    fun `빈 chunk 크기는 거부한다`() {
        check(runCatching { runStreamingSearchExample("secret", 0) }.isFailure)
    }
}
