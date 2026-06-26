package io.bluetape4k.text.examples.search

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldContain
import io.bluetape4k.assertions.shouldHaveSize
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.seconds

class TextSearchExamplesTest {

    @Test
    fun `runnable example covers builder dsl replacement and flow search`() = runTest(timeout = 30.seconds) {
        val report = runTextSearchExamples()

        report.builderMatchValues shouldBeEqualTo setOf("ACCOUNT_TAKEOVER", "PAYMENT_RISK")
        report.dslMatchKeywords shouldBeEqualTo setOf("password reset", "card declined")
        report.redactedLog shouldBeEqualTo "user requested [ACCOUNT_TAKEOVER] before [PAYMENT_RISK]"
        report.firstFlowAlert?.value shouldBeEqualTo "ACCOUNT_TAKEOVER"
    }

    @Test
    fun `flow example stops after the first alert near the beginning`() = runTest(timeout = 30.seconds) {
        val match = collectFirstAlert("critical login failed. " + "safe ".repeat(50_000))

        match?.keyword shouldBeEqualTo "critical login"
        match?.value shouldBeEqualTo "ACCOUNT_TAKEOVER"
    }

    @Test
    fun `flow example completes a bounded no match scan`() = runTest(timeout = 30.seconds) {
        val matches = collectBoundedNoMatch("safe ".repeat(1_000))

        matches shouldHaveSize 0
    }

    @Test
    fun `console output is useful for smoke checks`() = runTest(timeout = 30.seconds) {
        renderTextSearchExampleReport(runTextSearchExamples()) shouldContain "ACCOUNT_TAKEOVER"
    }
}
