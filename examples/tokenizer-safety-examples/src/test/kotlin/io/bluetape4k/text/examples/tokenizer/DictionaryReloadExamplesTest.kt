package io.bluetape4k.text.examples.tokenizer

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldContain
import org.junit.jupiter.api.Test

class DictionaryReloadExamplesTest {

    @Test
    fun `fixture reload changes blockword masking result`() {
        val report = runDictionaryReloadExample()

        report.beforeVersion.revision shouldBeEqualTo 1
        report.afterVersion.revision shouldBeEqualTo 2
        report.beforeMaskedText shouldBeEqualTo "*********** and fresh-word"
        report.afterMaskedText shouldBeEqualTo "*********** and **********"
        report.activeWords shouldBeEqualTo setOf("legacy-word", "fresh-word")
    }

    @Test
    fun `failed loader keeps the last successful snapshot`() {
        runDictionaryReloadExample().failedReloadPreserved.shouldBeTrue()
    }

    @Test
    fun `rendered report exposes version and failure isolation`() {
        val output = renderDictionaryReloadReport(runDictionaryReloadExample())

        output shouldContain "sample-blockwords@1"
        output shouldContain "sample-blockwords@2"
        output shouldContain "failedReloadPreserved=true"
    }
}
