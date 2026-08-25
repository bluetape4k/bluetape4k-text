package io.bluetape4k.text.examples.lingua

import com.github.pemistahl.lingua.api.Language
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeGreaterThan
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldContain
import io.bluetape4k.assertions.shouldContainAll
import io.bluetape4k.assertions.shouldHaveSize
import io.bluetape4k.assertions.shouldNotContain
import org.junit.jupiter.api.Test
import java.nio.file.Files

class OfflineCorpusQualityExamplesTest {

    @Test
    fun `checked in fixture runs language detection and tokenizer checks`() {
        val report = runOfflineCorpusQualityExample()

        report.source shouldBeEqualTo "resource:/quality/offline-corpus.tsv"
        report.observations shouldHaveSize 3
        report.passedCount shouldBeEqualTo 3
        report.failedCount shouldBeEqualTo 0
        report.isSuccessful.shouldBeTrue()
        report.observations.flatMap { it.detectedLanguages }.toSet()
            .shouldContainAll(Language.KOREAN, Language.JAPANESE)
        report.observations.forEach { observation ->
            observation.expectedLanguages.forEach { language ->
                (observation.tokenCounts[language] ?: 0) shouldBeGreaterThan 0
            }
        }
    }

    @Test
    fun `private corpus path replaces checked in fixture without echoing source text`() {
        val privateText = "개인 corpus 문장"
        val path = Files.createTempFile("bluetape4k-offline-corpus-", ".tsv")
        try {
            Files.writeString(path, "private-ko\tKOREAN\t$privateText\n")

            val report = runOfflineCorpusQualityExample(path)
            val rendered = renderOfflineCorpusQualityReport(report)

            report.source shouldBeEqualTo path.toString()
            report.observations.map { it.id } shouldBeEqualTo listOf("private-ko")
            report.isSuccessful.shouldBeTrue()
            rendered shouldContain "private-ko"
            rendered shouldContain "deterministic fixture smoke check"
            rendered shouldNotContain privateText
        } finally {
            Files.deleteIfExists(path)
        }
    }

    @Test
    fun `rendered report separates deterministic checks from benchmark claims`() {
        val rendered = renderOfflineCorpusQualityReport(runOfflineCorpusQualityExample())

        rendered shouldContain "| Case | Expected languages | Detected languages |"
        rendered shouldContain "PASS"
        rendered shouldContain "not an official NLP benchmark"
        rendered shouldNotContain "precision"
        rendered shouldNotContain "recall"
    }
}
