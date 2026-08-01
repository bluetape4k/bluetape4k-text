@file:Suppress("MatchingDeclarationName")

package io.bluetape4k.text.examples.tokenizer

import io.bluetape4k.tokenizer.utils.DictionarySnapshot
import io.bluetape4k.tokenizer.utils.DictionaryVersion
import io.bluetape4k.tokenizer.utils.VersionedDictionary
import java.io.Serial
import java.io.Serializable

/** 버전별 금칙어 reload 결과를 보여 주는 실행 가능한 예제 보고서입니다. */
internal data class DictionaryReloadExampleReport(
    /** reload 전 snapshot의 버전입니다. */
    val beforeVersion: DictionaryVersion,
    /** 성공한 reload 후 snapshot의 버전입니다. */
    val afterVersion: DictionaryVersion,
    /** 초기 금칙어 snapshot으로 처리한 결과입니다. */
    val beforeMaskedText: String,
    /** 갱신된 금칙어 snapshot으로 처리한 결과입니다. */
    val afterMaskedText: String,
    /** 실패한 reload 뒤에도 성공한 snapshot이 유지되었는지 여부입니다. */
    val failedReloadPreserved: Boolean,
    /** 성공한 reload 후 활성화된 금칙어 목록입니다. */
    val activeWords: Set<String>,
): Serializable {
    companion object {
        @Serial
        private const val serialVersionUID: Long = 1L
    }
}

/**
 * 작은 fixture 금칙어 사전으로 버전 갱신과 실패 격리를 시연합니다.
 *
 * 실제 서비스에서는 [VersionedDictionary]를 tokenizer provider나 blockword
 * processor의 원자적 snapshot 경계로 사용할 수 있습니다. 이 예제의 processor는
 * snapshot을 매 요청에 읽어 갱신 중간 상태를 노출하지 않습니다.
 *
 * @return 초기/갱신 결과, 활성 버전, 실패한 reload 보존 여부입니다.
 */
internal fun runDictionaryReloadExample(): DictionaryReloadExampleReport {
    val initial = DictionarySnapshot(
        DictionaryVersion("sample-blockwords", 1),
        loadDictionaryFixture("blockwords-v1.txt"),
    )
    val dictionary = VersionedDictionary(initial)
    val processor = RuntimeBlockwordProcessor(dictionary)
    val sampleText = "legacy-word and fresh-word"
    val beforeMaskedText = processor.mask(sampleText)

    val updated = dictionary.reload(DictionaryVersion("sample-blockwords", 2)) {
        loadDictionaryFixture("blockwords-v2.txt")
    }
    val afterMaskedText = processor.mask(sampleText)
    val failedReloadPreserved = runCatching {
        dictionary.reload(DictionaryVersion("sample-blockwords", 3)) {
            error("fixture loader failed")
        }
    }.isFailure && dictionary.snapshot() == updated

    return DictionaryReloadExampleReport(
        beforeVersion = initial.version,
        afterVersion = updated.version,
        beforeMaskedText = beforeMaskedText,
        afterMaskedText = afterMaskedText,
        failedReloadPreserved = failedReloadPreserved,
        activeWords = dictionary.snapshot().value,
    )
}

/** 예제 보고서를 명령행에서 읽기 쉬운 여러 줄 문자열로 렌더링합니다. */
internal fun renderDictionaryReloadReport(report: DictionaryReloadExampleReport): String = buildString {
    appendLine("before=${report.beforeVersion.name}@${report.beforeVersion.revision}: ${report.beforeMaskedText}")
    appendLine("after=${report.afterVersion.name}@${report.afterVersion.revision}: ${report.afterMaskedText}")
    appendLine("active=${report.activeWords.sorted().joinToString()}")
    appendLine("failedReloadPreserved=${report.failedReloadPreserved}")
}

private class RuntimeBlockwordProcessor(
    private val dictionary: VersionedDictionary<Set<String>>,
) {
    fun mask(text: String): String {
        val words = dictionary.snapshot().value.sortedByDescending { it.length }
        return words.fold(text) { current, word ->
            current.replace(word, "*".repeat(word.length), ignoreCase = true)
        }
    }
}

private fun loadDictionaryFixture(name: String): Set<String> =
    requireNotNull(DictionaryReloadExamples::class.java.getResourceAsStream("/dictionaries/$name")) {
        "Dictionary fixture not found: $name"
    }.bufferedReader().useLines { lines ->
        lines.map(String::trim).filter(String::isNotEmpty).toSet()
    }

private object DictionaryReloadExamples
