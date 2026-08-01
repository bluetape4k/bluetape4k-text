package io.bluetape4k.text.examples.search

import io.bluetape4k.text.search.AhoCorasickAutomaton
import io.bluetape4k.text.search.AhoCorasickMatch
import io.bluetape4k.text.search.SearchOptions
import io.bluetape4k.text.search.WordBoundary
import io.bluetape4k.text.search.ahoCorasick
import io.bluetape4k.text.search.flow.matchesAsFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.Serial
import java.io.Serializable
import kotlin.time.Duration.Companion.seconds

internal data class TextSearchExampleReport(
    val builderMatchValues: Set<String>,
    val dslMatchKeywords: Set<String>,
    val redactedLog: String,
    val firstFlowAlert: AhoCorasickMatch<String>?,
): Serializable {
    companion object {
        @Serial
        private const val serialVersionUID: Long = 1L
    }
}

internal suspend fun runTextSearchExamples(): TextSearchExampleReport {
    val logLine = "user requested password reset before card declined"
    val builderAutomaton = buildRiskAutomaton()
    val dslAutomaton = ahoCorasick<String> {
        ignoreCase = true
        allowOverlaps = false
        wordBoundary = WordBoundary.WHITESPACE_SEPARATED
        keyword("password reset", "ACCOUNT_TAKEOVER")
        keyword("card declined", "PAYMENT_RISK")
    }

    val builderMatches = builderAutomaton.parseText(logLine)
    val dslMatches = dslAutomaton.parseText(logLine)
    val redacted = builderAutomaton.replaceAll(logLine) { match -> "[${match.value}]" }
    val firstAlert = collectFirstAlert("critical login failed before card declined")

    return TextSearchExampleReport(
        builderMatchValues = builderMatches.mapTo(linkedSetOf()) { it.value },
        dslMatchKeywords = dslMatches.mapTo(linkedSetOf()) { it.keyword },
        redactedLog = redacted,
        firstFlowAlert = firstAlert,
    )
}

internal suspend fun collectFirstAlert(logLine: String): AhoCorasickMatch<String>? =
    withContext(Dispatchers.Default.limitedParallelism(1)) {
        withTimeout(5.seconds) {
            flowAlertAutomaton()
                .matchesAsFlow(logLine)
                .take(1)
                .toList()
                .firstOrNull()
        }
    }

internal suspend fun collectBoundedNoMatch(logLine: String): List<AhoCorasickMatch<String>> =
    withContext(Dispatchers.Default.limitedParallelism(1)) {
        withTimeout(5.seconds) {
            flowAlertAutomaton()
                .matchesAsFlow(logLine)
                .take(1)
                .toList()
        }
    }

internal fun renderTextSearchExampleReport(report: TextSearchExampleReport): String =
    buildString {
        appendLine("builder=${report.builderMatchValues.joinToString()}")
        appendLine("dsl=${report.dslMatchKeywords.joinToString()}")
        appendLine("redacted=${report.redactedLog}")
        appendLine("flow=${report.firstFlowAlert?.value ?: "none"}")
    }

private fun buildRiskAutomaton(): AhoCorasickAutomaton<String> =
    AhoCorasickAutomaton.builder<String>()
        .add("password reset", "ACCOUNT_TAKEOVER")
        .add("card declined", "PAYMENT_RISK")
        .options(SearchOptions(ignoreCase = true, allowOverlaps = false, wordBoundary = WordBoundary.WHITESPACE_SEPARATED))
        .build()

private fun flowAlertAutomaton(): AhoCorasickAutomaton<String> =
    ahoCorasick {
        ignoreCase = true
        keyword("critical login", "ACCOUNT_TAKEOVER")
        keyword("card declined", "PAYMENT_RISK")
    }

fun main() = runBlocking {
    println(renderTextSearchExampleReport(runTextSearchExamples()))
    println(
        renderStreamingSearchExample(
            runStreamingSearchExample("password reset before card declined and secret", chunkSize = 5)
        )
    )
}
