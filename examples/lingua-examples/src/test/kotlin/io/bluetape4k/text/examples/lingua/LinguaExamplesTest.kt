package io.bluetape4k.text.examples.lingua

import com.github.pemistahl.lingua.api.Language
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldContain
import org.junit.jupiter.api.Test

class LinguaExamplesTest {

    @Test
    fun `runnable example reuses detector and reports mixed languages`() {
        val report = runLinguaExamples()

        report.reusedDetectorLanguages shouldBeEqualTo setOf(Language.ENGLISH, Language.KOREAN, Language.JAPANESE)
        report.subsetLanguages shouldBeEqualTo setOf(Language.ENGLISH, Language.KOREAN, Language.JAPANESE)
        report.lowAccuracyLanguage shouldBeEqualTo Language.ENGLISH
    }

    @Test
    fun `console output documents reuse without timing claims`() {
        val output = renderLinguaExampleReport(runLinguaExamples())

        output shouldContain "reuse"
        output shouldContain "subset"
        output shouldContain "lowAccuracy"
    }
}
