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
 * [text]의 모든 키워드 match를 [Flow]로 반환하며 협력 취소를 지원합니다.
 *
 * `channelFlow + flowOn(Dispatchers.Default)`를 사용하므로 producer는 collector thread 밖에서 실행됩니다.
 * 기본 겹침/단어 경계 옵션에서는 trie traversal이 match를 찾는 즉시 전송하며, `take(N)`을 통한 조기 종료는
 * 모든 match를 materialize하지 않고 producer를 중단합니다.
 *
 * [SearchOptions.allowOverlaps]가 `false`이거나 [SearchOptions.wordBoundary]가 [WordBoundary.NONE]이 아니면
 * automaton은 전체 결과 후처리를 적용해야 합니다. 이 경우 이 함수는 필터링된 [parseText] 결과를 emit하되
 * send 사이에서 취소를 계속 확인합니다.
 *
 * **참고**: 여기서는 [io.bluetape4k.text.search.SearchOptions.stopOnFirstMatch]를 무시합니다.
 * 첫 match 뒤 중단하려면 Flow에 `take(1)`을 사용하세요.
 *
 * ```kotlin
 * val automaton = ahoCorasickOf("he", "she", "his", "hers")
 * val matches: List<AhoCorasickMatch<String>> = automaton.matchesAsFlow("ushers")
 *     .take(5)
 *     .toList()
 * ```
 *
 * @param V 각 키워드에 연결된 값 타입입니다.
 * @param text 검색할 입력 문자열입니다.
 * @return 키워드 match를 방출하는 [Flow]입니다.
 */
fun <V> AhoCorasickAutomaton<V>.matchesAsFlow(text: CharSequence): Flow<AhoCorasickMatch<V>> =
    channelFlow {
        if (options.requiresEagerFlowPostProcessing()) {
            for (match in parseTextSuspending(text, ignoreStopOnFirstMatch = true)) {
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
