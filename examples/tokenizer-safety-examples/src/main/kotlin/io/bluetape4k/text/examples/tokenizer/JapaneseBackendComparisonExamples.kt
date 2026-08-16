@file:Suppress("MatchingDeclarationName")

package io.bluetape4k.text.examples.tokenizer

import com.worksap.nlp.sudachi.Config
import com.worksap.nlp.sudachi.DictionaryFactory
import com.worksap.nlp.sudachi.PathAnchor
import com.worksap.nlp.sudachi.Tokenizer
import io.bluetape4k.tokenizer.japanese.JapaneseProcessor
import java.nio.file.Files
import java.nio.file.Path

private const val DEFAULT_COMPARISON_TEXT = "選挙管理委員会"
private const val SUDACHI_SYSTEM_DICTIONARY_PROPERTY = "bluetape4k.sudachi.system-dictionary"
private const val SUDACHI_DICTIONARY_VERSION = "20260428"
private const val SUDACHI_DICTIONARY_ARCHIVE_SHA256 =
    "40c8ffc095283f07aa06cae922e7b8147bf2919ec8830567b0b3f7a7efa3239f"
private const val SUDACHI_DICTIONARY_SIZE = 217_374_303L
private val COMPARISON_CORPUS = listOf(
    "選挙管理委員会",
    "東京都へ行く",
    "外国人参政権",
)

/** backend observation의 실행 상태를 기록합니다. */
internal enum class BackendExecution {
    LIVE,
    RECORDED,
}

/** backend POS 결과를 현재 report model에 연결했는지 나타내는 상태입니다. */
internal enum class PosMappingStatus {
    MAPPED,
    UNMAPPED,
}

internal data class JapaneseTokenObservation(
    val surface: String,
    val partOfSpeech: String,
)

internal data class CandidateSplitModeObservation(
    val mode: String,
    val surfaces: List<String>,
)

internal data class JapaneseBackendObservation(
    val backend: String,
    val dictionary: String,
    val license: String,
    val runtimeFootprint: String,
    val gradleDependency: String,
    val dictionaryVersion: String,
    val dictionaryArchiveSha256: String,
    val dictionarySize: Long,
    val execution: BackendExecution,
    val posMapping: PosMappingStatus,
    val tokens: List<JapaneseTokenObservation>,
    val splitModes: List<CandidateSplitModeObservation>,
)

internal data class JapaneseBackendComparisonReport(
    val input: String,
    val current: JapaneseBackendObservation,
    val candidate: JapaneseBackendObservation,
)

private data class SudachiAnalysis(
    val tokens: List<JapaneseTokenObservation>,
    val splitModes: List<CandidateSplitModeObservation>,
)

/**
 * 동일 입력에 대한 Kuromoji와 Sudachi의 실제 dictionary-backed 결과를 같은 report shape으로 보여 줍니다.
 *
 * SudachiDict binary는 저장소에 커밋하지 않고 Gradle build cache에서 준비합니다. 두 backend의
 * POS 체계는 중립 observation의 첫 번째 broad POS field로 연결하며, A/B/C surface 차이도 함께 남깁니다.
 */
internal fun runJapaneseBackendComparison(
    text: String = DEFAULT_COMPARISON_TEXT,
): JapaneseBackendComparisonReport {
    require(text in COMPARISON_CORPUS) {
        "The comparison fixture only supports the approved corpus: ${COMPARISON_CORPUS.joinToString()}."
    }

    val currentTokens = JapaneseProcessor.tokenize(text).map { token ->
        JapaneseTokenObservation(
            surface = token.surface,
            partOfSpeech = token.allFeaturesArray.firstOrNull().orEmpty(),
        )
    }
    val candidate = analyzeSudachi(text)

    return JapaneseBackendComparisonReport(
        input = text,
        current = kuromojiObservation(currentTokens),
        candidate = sudachiObservation(candidate),
    )
}

internal fun comparisonCorpus(): List<String> = COMPARISON_CORPUS

private fun analyzeSudachi(text: String): SudachiAnalysis {
    val dictionaryPath = sudachiSystemDictionaryPath()
    return openSudachiDictionary(dictionaryPath).use { dictionary ->
        val tokenizer = dictionary.create()
        val splitModes = listOf(
            "A" to Tokenizer.SplitMode.A,
            "B" to Tokenizer.SplitMode.B,
            "C" to Tokenizer.SplitMode.C,
        ).map { (mode, splitMode) ->
            CandidateSplitModeObservation(
                mode = mode,
                surfaces = tokenizer.tokenize(splitMode, text).map { it.surface() },
            )
        }
        val tokens = tokenizer.tokenize(Tokenizer.SplitMode.C, text).map { morpheme ->
            JapaneseTokenObservation(
                surface = morpheme.surface(),
                partOfSpeech = morpheme.partOfSpeech().firstOrNull().orEmpty(),
            )
        }
        SudachiAnalysis(tokens = tokens, splitModes = splitModes)
    }
}

private fun kuromojiObservation(tokens: List<JapaneseTokenObservation>) = JapaneseBackendObservation(
    backend = "Kuromoji IPADic",
    dictionary = "IPADic bundled in kuromoji-ipadic",
    license = "Apache-2.0",
    runtimeFootprint = "bundled IPADic artifact",
    gradleDependency = "bt4k.kuromoji.ipadic",
    dictionaryVersion = "bundled by kuromoji-ipadic",
    dictionaryArchiveSha256 = "not applicable",
    dictionarySize = -1L,
    execution = BackendExecution.LIVE,
    posMapping = PosMappingStatus.MAPPED,
    tokens = tokens,
    splitModes = emptyList(),
)

private fun sudachiObservation(analysis: SudachiAnalysis) = JapaneseBackendObservation(
    backend = "Sudachi JVM",
    dictionary = "external SudachiDict system dictionary",
    license = "Apache-2.0",
    runtimeFootprint = "external system_core.dic in build cache (not committed)",
    gradleDependency = "bt4k.sudachi",
    dictionaryVersion = "SudachiDict v$SUDACHI_DICTIONARY_VERSION core",
    dictionaryArchiveSha256 = SUDACHI_DICTIONARY_ARCHIVE_SHA256,
    dictionarySize = SUDACHI_DICTIONARY_SIZE,
    execution = BackendExecution.LIVE,
    posMapping = PosMappingStatus.MAPPED,
    tokens = analysis.tokens,
    splitModes = analysis.splitModes,
)

private fun sudachiSystemDictionaryPath(): Path {
    val configuredPath = System.getProperty(SUDACHI_SYSTEM_DICTIONARY_PROPERTY).orEmpty().trim()
    require(configuredPath.isNotEmpty()) {
        "Sudachi system dictionary is not configured; run prepareSudachiDictionary first."
    }
    return Path.of(configuredPath).also { path ->
        require(Files.isRegularFile(path)) { "Sudachi system dictionary is missing: $path" }
        require(Files.size(path) == SUDACHI_DICTIONARY_SIZE) {
            "Sudachi system dictionary has an unexpected size: $path"
        }
    }
}

private fun openSudachiDictionary(dictionaryPath: Path) =
    DictionaryFactory().create(
        Config.defaultConfig(PathAnchor.filesystem(dictionaryPath.parent)).systemDictionary(dictionaryPath),
    )

internal fun renderJapaneseBackendComparison(report: JapaneseBackendComparisonReport): String = buildString {
    appendLine("input=${report.input}")
    appendLine("current-backend=${report.current.backend}")
    appendLine("current-dictionary=${report.current.dictionary}")
    appendLine("current-license=${report.current.license}")
    appendLine("current-runtime-footprint=${report.current.runtimeFootprint}")
    appendLine("current-gradle-dependency=${report.current.gradleDependency}")
    appendLine("current-dictionary-version=${report.current.dictionaryVersion}")
    appendLine("current-execution=${report.current.execution}")
    appendLine("current-pos-mapping=${report.current.posMapping}")
    appendLine("current-tokens=${report.current.tokens.joinToString { "${it.surface}/${it.partOfSpeech}" }}")
    appendLine("candidate-backend=${report.candidate.backend}")
    appendLine("candidate-dictionary=${report.candidate.dictionary}")
    appendLine("candidate-license=${report.candidate.license}")
    appendLine("candidate-runtime-footprint=${report.candidate.runtimeFootprint}")
    appendLine("candidate-gradle-dependency=${report.candidate.gradleDependency}")
    appendLine("candidate-dictionary-version=${report.candidate.dictionaryVersion}")
    appendLine("candidate-dictionary-sha256=${report.candidate.dictionaryArchiveSha256}")
    appendLine("candidate-dictionary-size=${report.candidate.dictionarySize}")
    appendLine("candidate-execution=${report.candidate.execution}")
    val splitModes = report.candidate.splitModes.joinToString {
        "${it.mode}:${it.surfaces.joinToString("/")}"
    }
    appendLine("candidate-split-modes=$splitModes")
    appendLine("candidate-pos-mapping=${report.candidate.posMapping}")
    appendLine(
        "migration-note=compare same corpus and record mode-specific surface/POS mismatches before backend migration",
    )
}
