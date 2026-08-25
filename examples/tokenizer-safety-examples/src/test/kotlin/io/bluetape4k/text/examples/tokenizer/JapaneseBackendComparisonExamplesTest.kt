package io.bluetape4k.text.examples.tokenizer

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldHaveSize
import io.bluetape4k.assertions.shouldContain
import io.bluetape4k.assertions.shouldNotBeEmpty
import org.junit.jupiter.api.Test

class JapaneseBackendComparisonExamplesTest {

    @Test
    fun `comparison executes both dictionary backed backends`() {
        val report = runJapaneseBackendComparison()

        report.current.backend shouldBeEqualTo "Kuromoji IPADic"
        report.current.tokens.shouldNotBeEmpty()
        report.candidate.backend shouldBeEqualTo "Sudachi JVM"
        report.current.execution shouldBeEqualTo BackendExecution.LIVE
        report.candidate.execution shouldBeEqualTo BackendExecution.LIVE
        report.current.posMapping shouldBeEqualTo PosMappingStatus.MAPPED
        report.candidate.posMapping shouldBeEqualTo PosMappingStatus.MAPPED
        report.candidate.tokens.shouldNotBeEmpty()
        report.candidate.dictionaryVersion shouldBeEqualTo "SudachiDict v20260428 core"
        report.candidate.dictionarySize shouldBeEqualTo 217_374_303L
    }

    @Test
    fun `comparison captures the official Sudachi split mode contract`() {
        val report = runJapaneseBackendComparison("選挙管理委員会")

        report.candidate.splitModes.map { it.mode } shouldBeEqualTo listOf("A", "B", "C")
        report.candidate.splitModes.first { it.mode == "A" }.surfaces shouldBeEqualTo
            listOf("選挙", "管理", "委員", "会")
        report.candidate.splitModes.first { it.mode == "B" }.surfaces shouldBeEqualTo
            listOf("選挙", "管理", "委員会")
        report.candidate.splitModes.first { it.mode == "C" }.surfaces shouldBeEqualTo
            listOf("選挙管理委員会")
    }

    @Test
    fun `comparison uses one approved corpus and exposes segmentation mismatches`() {
        comparisonCorpus().map { runJapaneseBackendComparison(it) } shouldHaveSize 3

        val tokyo = runJapaneseBackendComparison("東京都へ行く")
        tokyo.current.tokens.map { it.surface } shouldBeEqualTo listOf("東京", "都", "へ", "行く")
        tokyo.candidate.splitModes.first { it.mode == "B" }.surfaces shouldBeEqualTo
            listOf("東京都", "へ", "行く")

        val foreign = runJapaneseBackendComparison("外国人参政権")
        foreign.current.tokens.map { it.surface } shouldBeEqualTo listOf("外国", "人参", "政権")
        foreign.candidate.splitModes.first { it.mode == "A" }.surfaces shouldBeEqualTo
            listOf("外国", "人", "参政", "権")
        foreign.candidate.splitModes.first { it.mode == "C" }.surfaces shouldBeEqualTo
            listOf("外国人参政権")
    }

    @Test
    fun `rendered comparison states dictionary and migration boundary`() {
        val output = renderJapaneseBackendComparison(runJapaneseBackendComparison())

        output shouldContain "Kuromoji IPADic"
        output shouldContain "Sudachi JVM"
        output shouldContain "candidate-execution=LIVE"
        output shouldContain "candidate-pos-mapping=MAPPED"
        output shouldContain "candidate-license=Apache-2.0"
        output shouldContain "candidate-gradle-dependency=bt4k.sudachi"
        output shouldContain
            "candidate-dictionary-sha256=40c8ffc095283f07aa06cae922e7b8147bf2919ec8830567b0b3f7a7efa3239f"
        output shouldContain "candidate-dictionary=external"
    }
}
