@file:Suppress("MatchingDeclarationName")

package io.bluetape4k.text.examples.tokenizer

import io.bluetape4k.tokenizer.japanese.JapaneseProcessor

private const val DEFAULT_COMPARISON_TEXT = "選挙管理委員会"

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

/**
 * 현재 Kuromoji 결과와 Sudachi의 공식 split-mode surface 기록을 같은 report shape으로 보여 줍니다.
 *
 * 후보 쪽은 Sudachi system dictionary를 저장소에 포함하지 않으므로 실행 결과가 아닙니다.
 * 따라서 POS mapping을 의도적으로 [PosMappingStatus.UNMAPPED]로 남겨 migration 경계를 드러냅니다.
 */
internal fun runJapaneseBackendComparison(
    text: String = DEFAULT_COMPARISON_TEXT,
): JapaneseBackendComparisonReport {
    require(text == DEFAULT_COMPARISON_TEXT) {
        "The comparison fixture only supports '$DEFAULT_COMPARISON_TEXT'."
    }

    val currentTokens = JapaneseProcessor.tokenize(text).map { token ->
        JapaneseTokenObservation(
            surface = token.surface,
            partOfSpeech = token.allFeaturesArray.firstOrNull().orEmpty(),
        )
    }

    return JapaneseBackendComparisonReport(
        input = text,
        current = JapaneseBackendObservation(
            backend = "Kuromoji IPADic",
            dictionary = "IPADic bundled in kuromoji-ipadic",
            license = "Apache-2.0",
            runtimeFootprint = "bundled IPADic artifact",
            gradleDependency = "bt4k.kuromoji.ipadic",
            execution = BackendExecution.LIVE,
            posMapping = PosMappingStatus.MAPPED,
            tokens = currentTokens,
            splitModes = emptyList(),
        ),
        candidate = JapaneseBackendObservation(
            backend = "Sudachi JVM",
            dictionary = "external SudachiDict system dictionary",
            license = "Apache-2.0",
            runtimeFootprint = "external system_core.dic (not committed)",
            gradleDependency = "not added",
            execution = BackendExecution.RECORDED,
            posMapping = PosMappingStatus.UNMAPPED,
            tokens = emptyList(),
            splitModes = listOf(
                CandidateSplitModeObservation("A", listOf("選挙", "管理", "委員", "会")),
                CandidateSplitModeObservation("B", listOf("選挙", "管理", "委員会")),
                CandidateSplitModeObservation("C", listOf("選挙管理委員会")),
            ),
        ),
    )
}

internal fun renderJapaneseBackendComparison(report: JapaneseBackendComparisonReport): String = buildString {
    appendLine("input=${report.input}")
    appendLine("current-backend=${report.current.backend}")
    appendLine("current-dictionary=${report.current.dictionary}")
    appendLine("current-license=${report.current.license}")
    appendLine("current-runtime-footprint=${report.current.runtimeFootprint}")
    appendLine("current-gradle-dependency=${report.current.gradleDependency}")
    appendLine("current-execution=${report.current.execution}")
    appendLine("current-pos-mapping=${report.current.posMapping}")
    appendLine("current-tokens=${report.current.tokens.joinToString { "${it.surface}/${it.partOfSpeech}" }}")
    appendLine("candidate-backend=${report.candidate.backend}")
    appendLine("candidate-dictionary=${report.candidate.dictionary}")
    appendLine("candidate-license=${report.candidate.license}")
    appendLine("candidate-runtime-footprint=${report.candidate.runtimeFootprint}")
    appendLine("candidate-gradle-dependency=${report.candidate.gradleDependency}")
    appendLine("candidate-execution=${report.candidate.execution}")
    val splitModes = report.candidate.splitModes.joinToString {
        "${it.mode}:${it.surfaces.joinToString("/")}"
    }
    appendLine("candidate-split-modes=$splitModes")
    appendLine("candidate-pos-mapping=${report.candidate.posMapping}")
    appendLine("migration-note=add dictionary-backed adapter before claiming runtime parity")
}
