@file:Suppress("MatchingDeclarationName")

package io.bluetape4k.text.examples.search

import io.bluetape4k.text.search.AhoCorasickAutomaton
import io.bluetape4k.text.search.AhoCorasickMatch
import io.bluetape4k.text.search.SearchOptions
import io.bluetape4k.text.search.WordBoundary

/** eager 검색과 청크 검색 결과를 함께 보여 주는 예제 보고서입니다. */
data class StreamingSearchExampleReport(
    val eager: List<AhoCorasickMatch<String>>,
    val streamed: List<AhoCorasickMatch<String>>,
    val equivalent: Boolean,
)

/**
 * 고정된 청크 크기로 입력을 나누어 scanner 결과와 일반 parseText 결과를 비교합니다.
 *
 * @param text 검색할 전체 입력입니다.
 * @param chunkSize 한 번에 전달할 UTF-16 문자 수입니다.
 * @return 두 검색 경로의 결과와 동등성입니다.
 */
fun runStreamingSearchExample(
    text: String,
    chunkSize: Int = 7,
): StreamingSearchExampleReport {
    require(chunkSize > 0) { "chunkSize must be positive" }
    val automaton = streamingExampleAutomaton()
    val scanner = automaton.scanner()
    val streamed = buildList {
        text.chunked(chunkSize).forEach { addAll(scanner.scan(it)) }
        addAll(scanner.finish())
    }
    val eager = automaton.parseText(text)
    return StreamingSearchExampleReport(eager, streamed, eager == streamed)
}

fun renderStreamingSearchExample(report: StreamingSearchExampleReport): String = buildString {
    appendLine("equivalent=${report.equivalent}")
    report.streamed.forEach { match ->
        appendLine("${match.keyword} ${match.start}..${match.end}: ${match.value}")
    }
}

private fun streamingExampleAutomaton(): AhoCorasickAutomaton<String> =
    AhoCorasickAutomaton.builder<String>()
        .add("password reset", "ACCOUNT_TAKEOVER")
        .add("card declined", "PAYMENT_RISK")
        .add("secret", "SECRET_DATA")
        .options(
            SearchOptions(
                ignoreCase = true,
                allowOverlaps = false,
                wordBoundary = WordBoundary.WHITESPACE_SEPARATED,
            )
        )
        .build()
