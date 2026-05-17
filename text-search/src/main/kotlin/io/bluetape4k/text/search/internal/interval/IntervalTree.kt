package io.bluetape4k.text.search.internal.interval

import io.bluetape4k.ValueObject
import io.bluetape4k.text.search.internal.interval.IntervalableComparators.PositionComparator
import io.bluetape4k.text.search.internal.interval.IntervalableComparators.ReverseSizeComparator
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.trace

/**
 * Binary interval tree for efficiently finding and removing overlapping intervals.
 *
 * Used by the Aho-Corasick automaton to eliminate overlapping keyword matches.
 *
 * ## Behavior / Contract
 * - [findOverlaps] returns overlapping intervals sorted by start position.
 * - [removeOverlaps] keeps larger intervals first and removes smaller overlapping ones.
 * - The input collection is never mutated; results are returned as a new list.
 *
 * ```kotlin
 * val tree = IntervalTree(listOf(Interval(0, 2), Interval(1, 3)))
 * // tree.removeOverlaps(listOf(Interval(0, 2), Interval(1, 3))).size == 1
 * ```
 *
 * @property rootNode root node of the tree
 * @see IntervalNode
 * @see Intervalable
 */
internal class IntervalTree private constructor(
    private val rootNode: IntervalNode,
): ValueObject {
    companion object: KLogging() {
        /**
         * Creates an [IntervalTree] from a pre-built [rootNode].
         */
        @JvmStatic
        operator fun invoke(rootNode: IntervalNode): IntervalTree = IntervalTree(rootNode)

        /**
         * Creates an [IntervalTree] by building an [IntervalNode] from [intervals].
         */
        @JvmStatic
        operator fun invoke(intervals: List<Intervalable>): IntervalTree = invoke(IntervalNode(intervals))
    }

    /**
     * Returns all intervals in the tree that overlap with [interval], sorted by start position.
     */
    fun findOverlaps(interval: Intervalable): List<Intervalable> =
        rootNode.findOverlaps(interval).sortedWith(PositionComparator)

    /**
     * Removes overlapping intervals from [intervals], keeping larger intervals over smaller ones.
     *
     * Results are sorted by start position.
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

        // overlap 된 interval들을 삭제합니다.
        log.trace { "overlap 된 interval들을 삭제=$removed" }
        results.removeAll(removed)

        // sort the intervals, now on left-most position only
        results.sortWith(PositionComparator)
        return results
    }
}
