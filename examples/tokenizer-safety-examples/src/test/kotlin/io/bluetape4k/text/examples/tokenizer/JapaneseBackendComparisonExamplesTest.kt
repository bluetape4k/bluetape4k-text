package io.bluetape4k.text.examples.tokenizer

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldContain
import io.bluetape4k.assertions.shouldNotBeEmpty
import org.junit.jupiter.api.Test

class JapaneseBackendComparisonExamplesTest {

    @Test
    fun `comparison executes Kuromoji and keeps candidate recording explicit`() {
        val report = runJapaneseBackendComparison()

        report.current.backend shouldBeEqualTo "Kuromoji IPADic"
        report.current.tokens.shouldNotBeEmpty()
        report.candidate.backend shouldBeEqualTo "Sudachi JVM"
        report.current.execution shouldBeEqualTo BackendExecution.LIVE
        report.candidate.execution shouldBeEqualTo BackendExecution.RECORDED
        report.current.posMapping shouldBeEqualTo PosMappingStatus.MAPPED
        report.candidate.posMapping shouldBeEqualTo PosMappingStatus.UNMAPPED
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
    fun `rendered comparison states dictionary and migration boundary`() {
        val output = renderJapaneseBackendComparison(runJapaneseBackendComparison())

        output shouldContain "Kuromoji IPADic"
        output shouldContain "Sudachi JVM"
        output shouldContain "candidate-execution=RECORDED"
        output shouldContain "pos-mapping=UNMAPPED"
        output shouldContain "candidate-license=Apache-2.0"
        output shouldContain "candidate-gradle-dependency=not added"
        output shouldContain "candidate-dictionary=external"
    }
}
