package io.bluetape4k.text.examples.lingua

import com.github.pemistahl.lingua.api.Language
import com.github.pemistahl.lingua.api.LanguageDetector
import io.bluetape4k.support.requireNotBlank
import java.io.Serial
import java.io.Reader
import java.io.Serializable
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.Locale

private const val OFFLINE_CORPUS_RESOURCE = "/quality/offline-corpus.tsv"
private const val OFFLINE_CORPUS_PATH_PROPERTY = "bluetape4k.offline-corpus.path"
private const val CORPUS_FIELD_COUNT = 3
private const val REPORT_CAVEAT =
    "This deterministic fixture is a consumer smoke check, not an official NLP benchmark."

/** offline fixture의 한 입력과 기대 언어 집합입니다. */
data class OfflineCorpusCase(
    val id: String,
    val expectedLanguages: Set<Language>,
    val text: String,
): Serializable {
    companion object {
        @Serial
        private const val serialVersionUID: Long = 1L
    }
}

/** 한 fixture를 실행한 감지·토큰화 결과입니다. 원문은 report에 복사하지 않습니다. */
data class OfflineCorpusObservation(
    val id: String,
    val expectedLanguages: Set<Language>,
    val detectedLanguages: Set<Language>,
    val tokenCounts: Map<Language, Int>,
    val passed: Boolean,
): Serializable {
    companion object {
        @Serial
        private const val serialVersionUID: Long = 1L
    }
}

/** 작은 offline corpus 실행 결과와 benchmark 범위 제한을 함께 보존합니다. */
data class OfflineCorpusQualityReport(
    val source: String,
    val observations: List<OfflineCorpusObservation>,
    val caveat: String = REPORT_CAVEAT,
): Serializable {
    companion object {
        @Serial
        private const val serialVersionUID: Long = 1L
    }

    val passedCount: Int
        get() = observations.count { it.passed }

    val failedCount: Int
        get() = observations.size - passedCount

    val isSuccessful: Boolean
        get() = failedCount == 0
}

/**
 * 기본 fixture 또는 `bluetape4k.offline-corpus.path`로 지정한 private corpus를 실행합니다.
 *
 * 입력 파일은 `id<TAB>LANGUAGE[,LANGUAGE...]<TAB>text` 형식이며, 빈 줄과 `#` 주석은
 * 무시합니다. 결과는 감지 언어와 언어별 token count만 포함하므로 원문 corpus를 report에
 * 다시 출력하지 않습니다.
 *
 * @param corpusPath private corpus 경로입니다. `null`이면 저장소 fixture를 사용합니다.
 * @param detector 여러 case에서 재사용할 Lingua detector입니다.
 */
fun runOfflineCorpusQualityExample(
    corpusPath: Path? = configuredOfflineCorpusPath(),
    detector: LanguageDetector = createMixedLanguageDetector(preloadModels = true),
): OfflineCorpusQualityReport {
    val (source, corpus) = corpusPath?.let { path ->
        require(Files.isRegularFile(path)) {
            "Offline corpus file does not exist: $path"
        }
        path.toString() to Files.newBufferedReader(path, StandardCharsets.UTF_8).use(::readOfflineCorpus)
    } ?: ("resource:$OFFLINE_CORPUS_RESOURCE" to loadOfflineCorpusFixture())

    val observations = corpus.map { entry ->
        val result = runMixedLanguagePipeline(entry.text, detector)
        val detectedLanguages = result.segments.mapTo(linkedSetOf()) { it.language }
        val tokenCounts = result.tokenCounts
        val detectedExpectedLanguages = entry.expectedLanguages.all(detectedLanguages::contains)
        val tokenizedExpectedLanguages = entry.expectedLanguages.all { language ->
            (tokenCounts[language] ?: 0) > 0
        }

        OfflineCorpusObservation(
            id = entry.id,
            expectedLanguages = entry.expectedLanguages,
            detectedLanguages = detectedLanguages,
            tokenCounts = tokenCounts,
            passed = detectedExpectedLanguages && tokenizedExpectedLanguages,
        )
    }

    return OfflineCorpusQualityReport(source = source, observations = observations)
}

/**
 * 실행 결과를 Markdown 표로 렌더링합니다. 이 출력은 fixture smoke check의 재현 결과이며
 * 외부 corpus의 precision, recall, F1 또는 성능을 주장하지 않습니다.
 */
fun renderOfflineCorpusQualityReport(report: OfflineCorpusQualityReport): String = buildString {
    appendLine("# Offline corpus quality sample")
    appendLine()
    appendLine("- Source: `${report.source}`")
    appendLine("- Cases: ${report.observations.size}")
    appendLine("- Passed: ${report.passedCount}")
    appendLine("- Failed: ${report.failedCount}")
    appendLine("- Scope: deterministic fixture smoke check")
    appendLine()
    appendLine("| Case | Expected languages | Detected languages | Korean tokens | Japanese tokens | Result |")
    appendLine("| --- | --- | --- | ---: | ---: | --- |")
    report.observations.forEach { observation ->
        appendLine(
            "| ${observation.id} | ${observation.expectedLanguages.renderLanguages()} | " +
                "${observation.detectedLanguages.renderLanguages()} | " +
                "${observation.tokenCounts[Language.KOREAN] ?: 0} | " +
                "${observation.tokenCounts[Language.JAPANESE] ?: 0} | " +
                "${if (observation.passed) "PASS" else "FAIL"} |"
        )
    }
    appendLine()
    appendLine("> ${report.caveat}")
}

private fun configuredOfflineCorpusPath(): Path? =
    System.getProperty(OFFLINE_CORPUS_PATH_PROPERTY)
        .orEmpty()
        .trim()
        .takeIf { it.isNotEmpty() }
        ?.let(Path::of)

private fun loadOfflineCorpusFixture(): List<OfflineCorpusCase> =
    requireNotNull(OfflineCorpusQualityExamples::class.java.getResourceAsStream(OFFLINE_CORPUS_RESOURCE)) {
        "Offline corpus fixture not found: $OFFLINE_CORPUS_RESOURCE"
    }.bufferedReader(StandardCharsets.UTF_8).use(::readOfflineCorpus)

private fun readOfflineCorpus(reader: Reader): List<OfflineCorpusCase> =
    reader.buffered().useLines { lines ->
        lines.mapIndexedNotNull { index, rawLine ->
            val line = rawLine.trim()
            if (line.isEmpty() || line.startsWith("#")) {
                return@mapIndexedNotNull null
            }

            val fields = line.split('\t', limit = CORPUS_FIELD_COUNT)
            require(fields.size == CORPUS_FIELD_COUNT) {
                "Offline corpus line ${index.inc()} must contain id, languages, and text"
            }
            val id = fields[0].trim().requireNotBlank("id")
            val expectedLanguages = fields[1]
                .split(',')
                .map { value ->
                    val languageName = value.trim().requireNotBlank("expected language")
                    Language.valueOf(languageName.uppercase(Locale.ROOT))
                }
                .toSet()
            val text = fields[2].trim().requireNotBlank("text")
            OfflineCorpusCase(id, expectedLanguages, text)
        }.toList().also { corpus ->
            require(corpus.isNotEmpty()) { "Offline corpus must contain at least one case" }
        }
    }

private fun Set<Language>.renderLanguages(): String =
    sortedBy(Language::name).joinToString(",") { it.name }

private object OfflineCorpusQualityExamples

fun main() {
    println(renderOfflineCorpusQualityReport(runOfflineCorpusQualityExample()))
}
