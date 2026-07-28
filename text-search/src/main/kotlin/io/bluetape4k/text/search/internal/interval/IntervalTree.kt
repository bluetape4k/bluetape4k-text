package io.bluetape4k.text.search.internal.interval

import io.bluetape4k.ValueObject
import io.bluetape4k.text.search.internal.interval.IntervalableComparators.PositionComparator
import io.bluetape4k.text.search.internal.interval.IntervalableComparators.ReverseSizeComparator
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.trace

/**
 * 겹치는 구간을 찾고 제거하기 위한 binary interval tree입니다.
 *
 * Aho-Corasick automaton에서 겹치는 키워드 match를 제거할 때 사용합니다.
 *
 * ## 동작 계약
 * - [findOverlaps]는 겹치는 구간을 start 위치 오름차순으로 반환합니다.
 * - [removeOverlaps]는 큰 구간을 우선 보존하고, 그보다 작은 겹침 구간을 제거합니다.
 * - 입력 collection은 변경하지 않고 결과를 새 list로 반환합니다.
 *
 * ```kotlin
 * val tree = IntervalTree(listOf(Interval(0, 2), Interval(1, 3)))
 * // tree.removeOverlaps(listOf(Interval(0, 2), Interval(1, 3))).size == 1
 * ```
 *
 * @property rootNode tree의 root node입니다.
 * @see IntervalNode
 * @see Intervalable
 */
internal class IntervalTree private constructor(
    private val rootNode: IntervalNode,
): ValueObject {
    companion object: KLogging() {
        /**
         * 이미 구성된 [rootNode]로 [IntervalTree]를 생성합니다.
         *
         * @param rootNode tree root로 사용할 node입니다.
         * @return [rootNode]를 감싼 [IntervalTree]입니다.
         */
        @JvmStatic
        operator fun invoke(rootNode: IntervalNode): IntervalTree = IntervalTree(rootNode)

        /**
         * [intervals]로 [IntervalNode]를 구성한 뒤 [IntervalTree]를 생성합니다.
         *
         * @param intervals tree에 넣을 구간 list입니다.
         * @return [intervals]를 담은 [IntervalTree]입니다.
         */
        @JvmStatic
        operator fun invoke(intervals: List<Intervalable>): IntervalTree = invoke(IntervalNode(intervals))
    }

    /**
     * Tree에서 [interval]과 겹치는 모든 구간을 start 위치 오름차순으로 반환합니다.
     *
     * @param interval 겹침을 검사할 기준 구간입니다.
     * @return [interval]과 겹치는 구간 list입니다.
     */
    fun findOverlaps(interval: Intervalable): List<Intervalable> =
        rootNode.findOverlaps(interval).sortedWith(PositionComparator)

    /**
     * [intervals]에서 겹치는 구간을 제거합니다. 작은 구간보다 큰 구간을 우선 보존합니다.
     *
     * 결과는 start 위치 오름차순으로 정렬합니다.
     *
     * @param T 처리할 구간 type입니다.
     * @param intervals 겹침 제거 대상 구간 collection입니다.
     * @return 겹침을 제거하고 start 위치 오름차순으로 정렬한 mutable list입니다.
     */
    fun <T: Intervalable> removeOverlaps(intervals: Collection<T>): MutableList<T> {
        // size가 큰 것부터
        val results = intervals.sortedWith(ReverseSizeComparator).toMutableList()
        val removed = mutableSetOf<Intervalable>()

        // 꼭 Sequence 방식으로 수행해야 updated된 removed를 사용할 수 있습니다.
        results
            .asSequence()
            .filterNot { removed.contains(it) }
            .forEach { target ->
                val overlaps = findOverlaps(target)
                log.debug { "target=$target, overlaps=$overlaps" }
                removed.addAll(overlaps)
            }

        // 겹친 interval들을 삭제합니다.
        log.trace { "겹친 interval들을 삭제=$removed" }
        results.removeAll(removed)

        // 남은 구간을 왼쪽 위치 기준으로만 정렬합니다.
        results.sortWith(PositionComparator)
        return results
    }
}
