package io.bluetape4k.text.search.flow

import io.bluetape4k.text.search.AhoCorasickAutomaton
import io.bluetape4k.text.search.AhoCorasickMatch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn

/**
 * Returns all keyword matches in [text] as a [Flow], supporting cooperative cancellation.
 *
 * Uses `channelFlow + flowOn(Dispatchers.Default)` so the producer respects collector backpressure.
 * Early termination via `take(N)` stops the producer immediately.
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
        val matches = parseText(text)
        for (match in matches) {
            currentCoroutineContext().ensureActive()
            send(match)
        }
    }.flowOn(Dispatchers.Default)
