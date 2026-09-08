package io.bluetape4k.text.search

/**
 * 청크 입력을 순차적으로 검색하는 상태 보존 scanner입니다.
 *
 * Scanner는 키워드 최대 길이에 비례하는 제한된 꼬리만 보관하고, 다음 청크가 와야 결과가 확정되는 구간은
 * 지연한다. 따라서 전체 입력을 하나의 문자열로 누적하지 않는다. 검색 결과의 offset은 모든
 * 청크를 이어 붙인 가상 입력 기준이며 [AhoCorasickMatch.end]는 기존 API와 같은 inclusive offset이다.
 *
 * 정규화는 청크 경계에서 원문 매핑을 보장할 수 없으므로 [NormalizationForm.NONE]만 지원한다.
 * `ignoreCase`, overlap, word-boundary 옵션은 일반 [AhoCorasickAutomaton.parseText]와 같은 규칙으로 적용한다.
 * 결과 순서는 eager API의 start ASC 정렬을 다시 적용하지 않고, 각 청크에서 확정된 raw trie traversal 순서를
 * 유지한다.
 *
 * @param V 키워드에 연결된 값의 타입입니다.
 * @property automaton 청크를 검색할 불변 automaton입니다.
 */
class AhoCorasickScanner<V> internal constructor(
    private val automaton: AhoCorasickAutomaton<V>,
) {
    private data class MatchKey(
        val start: Int,
        val end: Int,
        val keyword: String,
    )

    private var pending = ""
    private var pendingStart = 0
    private var inputLength = 0
    private val emitted = mutableSetOf<MatchKey>()

    init {
        require(automaton.options.normalization == NormalizationForm.NONE) {
            "Streaming scanner requires SearchOptions.normalization == NONE"
        }
    }

    /**
     * [chunk]을 현재 상태에 이어 붙여 확정된 match를 반환합니다.
     *
     * @param chunk 새로 도착한 입력 청크입니다. 빈 청크는 상태를 변경하지 않습니다.
     * @return 이번 호출에서 새로 확정된 match 목록입니다.
     */
    fun scan(chunk: CharSequence): List<AhoCorasickMatch<V>> = if (chunk.isEmpty()) {
        emptyList()
    } else {
        inputLength += chunk.length
        if (automaton.isEmpty()) {
            emptyList()
        } else {
            val combined = pending + chunk
            val safeEndExclusive = (combined.length - automaton.maxKeywordLength).coerceAtLeast(0)
            val matches = emitMatches(
                text = combined,
                baseOffset = pendingStart,
                maxEndExclusive = safeEndExclusive,
            )

            // 아직 끝 위치가 확정되지 않은 match의 시작점까지 포함하도록 최대 길이의 두 배와 경계 문자 하나를 보관합니다.
            val retainLength = automaton.maxKeywordLength * 2 + 1
            val retainStart = (combined.length - retainLength).coerceAtLeast(0)
            pending = combined.substring(retainStart)
            pendingStart += retainStart
            purgeEmittedBefore(pendingStart)
            matches
        }
    }

    /**
     * 지금까지 남겨 둔 꼬리를 모두 검색해 마지막 match를 반환합니다.
     *
     * 호출 뒤에도 scanner 객체는 재사용할 수 있으며, 다음 [scan]은 기존 입력 뒤의 offset을 사용합니다.
     * 새 입력 스트림을 시작하려면 [reset]을 호출합니다.
     *
     * @return 아직 확정되지 않았던 마지막 match 목록입니다.
     */
    fun finish(): List<AhoCorasickMatch<V>> {
        if (pending.isEmpty() || automaton.isEmpty()) {
            pending = ""
            pendingStart = inputLength
            emitted.clear()
            return emptyList()
        }
        val matches = emitMatches(pending, pendingStart, null)
        pending = ""
        pendingStart = inputLength
        emitted.clear()
        return matches
    }

    /** 현재 scanner 상태와 offset을 초기화합니다. */
    fun reset() {
        pending = ""
        pendingStart = 0
        inputLength = 0
        emitted.clear()
    }

    private fun emitMatches(
        text: CharSequence,
        baseOffset: Int,
        maxEndExclusive: Int?,
    ): List<AhoCorasickMatch<V>> {
        return automaton.parseTextStreaming(text).mapNotNull { match ->
            if (maxEndExclusive != null && match.end + 1 > maxEndExclusive) {
                return@mapNotNull null
            }
            val global = match.copy(
                start = baseOffset + match.start,
                end = baseOffset + match.end,
            )
            val key = MatchKey(global.start, global.end, global.keyword)
            if (emitted.add(key)) global else null
        }
    }

    private fun purgeEmittedBefore(offset: Int) {
        emitted.removeIf { it.end < offset }
    }
}
