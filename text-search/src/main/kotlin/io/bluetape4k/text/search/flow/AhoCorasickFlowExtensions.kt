package io.bluetape4k.text.search.flow

import io.bluetape4k.text.search.AhoCorasickAutomaton
import io.bluetape4k.text.search.AhoCorasickMatch
import io.bluetape4k.text.search.SearchOptions
import io.bluetape4k.text.search.WordBoundary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn

/**
 * Returns all keyword matches in [text] as a [Flow], supporting cooperative cancellation.
 *
 * Uses `channelFlow + flowOn(Dispatchers.Default)` so the producer runs off the collector thread.
 * For the default overlap/word-boundary options, matches are sent as the trie traversal finds them,
 * and early termination via `take(N)` stops the producer without materializing all matches.
 *
 * When [SearchOptions.allowOverlaps] is `false` or [SearchOptions.wordBoundary] is not [WordBoundary.NONE],
 * the automaton must apply whole-result post-processing, so this function emits the filtered [parseText]
 * result while still checking cancellation between sends.
 *
 * **Note**: [io.bluetape4k.text.search.SearchOptions.stopOnFirstMatch] is ignored here.
 * Use `take(1)` on the Flow to stop after the first match.
 *
 * ```kotlin
 * val automaton = ahoCorasickOf("he", "she", "his", "hers")
 * val matches: List<AhoCorasickMatch<String>> = automaton.matchesAsFlow("ushers")
 *     .take(5)
 *     .toList()
 * ```
 *
 * @param V type of value associated with each keyword
 * @param text input text to search
 */
fun <V> AhoCorasickAutomaton<V>.matchesAsFlow(text: CharSequence): Flow<AhoCorasickMatch<V>> =
    channelFlow {
        if (options.requiresEagerFlowPostProcessing()) {
            for (match in parseText(text)) {
                coroutineContext.ensureActive()
                send(match)
            }
            return@channelFlow
        }

        forEachRawMatch(text, ignoreStopOnFirstMatch = true) { match ->
            coroutineContext.ensureActive()
            send(match)
        }
    }.flowOn(Dispatchers.Default)

private fun SearchOptions.requiresEagerFlowPostProcessing(): Boolean =
    !allowOverlaps || wordBoundary != WordBoundary.NONE
