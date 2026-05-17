package io.bluetape4k.text.search.internal.interval

import io.bluetape4k.AbstractValueObject
import io.bluetape4k.support.hashOf
import java.util.*

/**
 * A node in the interval tree that partitions intervals around a [median] value.
 *
 * @param inputs the collection of intervals to store in this node
 * @see IntervalTree
 */
internal class IntervalNode(
    inputs: Collection<Intervalable>,
): AbstractValueObject() {
    /** Tree traversal direction. */
    enum class Direction {
        LEFT,
        RIGHT
    }

    /** Left child node. */
    var left: IntervalNode? = null

    /** Right child node. */
    var right: IntervalNode? = null

    /** Intervals stored at this node (those that cross the [median]). */
    val intervals = LinkedList<Intervalable>()

    /** Median value used to partition child intervals left and right. */
    val median: Int

    init {
        median = determineMedian(inputs)
        buildTree(inputs)
    }

    /**
     * Computes the median of the [inputs] collection as `(minStart + maxEnd) / 2`.
     */
    private fun determineMedian(inputs: Collection<Intervalable>): Int {
        val start = inputs.minOfOrNull { it.start } ?: 0
        val end = inputs.maxOfOrNull { it.end } ?: 0
        return (start + end) / 2
    }

    /**
     * Partitions [inputs] into left child, right child, and this node's [intervals] based on [median].
     */
    private fun buildTree(inputs: Collection<Intervalable>) {
        if (inputs.isEmpty()) {
            return
        }

        val toLeft = mutableListOf<Intervalable>()
        val toRight = mutableListOf<Intervalable>()

        inputs.forEach { input ->
            when {
                input.end < median -> toLeft.add(input)
                input.start > median -> toRight.add(input)
                else               -> intervals.add(input)
            }
        }
        if (toLeft.isNotEmpty()) {
            this.left = IntervalNode(toLeft)
        }
        if (toRight.isNotEmpty()) {
            this.right = IntervalNode(toRight)
        }
    }

    /**
     * Returns all intervals in this subtree that overlap with [interval], appending results to [destination].
     */
    fun findOverlaps(
        interval: Intervalable,
        destination: MutableList<Intervalable> = mutableListOf(),
    ): MutableList<Intervalable> {
        when {
            interval.start > median -> {
                addToOverlaps(interval, destination, findOverlappingRanges(right, interval))
                addToOverlaps(interval, destination, checkForOverlapsToRight(interval))
            }

            interval.end < median -> {
                addToOverlaps(interval, destination, findOverlappingRanges(left, interval))
                addToOverlaps(interval, destination, checkForOverlapsToLeft(interval))
            }

            else                  -> {
                addToOverlaps(interval, destination, this.intervals)
                addToOverlaps(interval, destination, findOverlappingRanges(left, interval))
                addToOverlaps(interval, destination, findOverlappingRanges(right, interval))
            }
        }
        return destination
    }

    /** Appends [newOverlaps] to [overlaps], excluding [interval] itself. */
    private fun addToOverlaps(
        interval: Intervalable,
        overlaps: MutableList<Intervalable>,
        newOverlaps: List<Intervalable>,
    ) {
        overlaps.addAll(newOverlaps.filter { it != interval })
    }

    /** Checks this node's intervals for overlaps with [interval] on the left side. */
    private fun checkForOverlapsToLeft(interval: Intervalable): List<Intervalable> =
        checkForOverlaps(interval, Direction.LEFT)

    /** Checks this node's intervals for overlaps with [interval] on the right side. */
    private fun checkForOverlapsToRight(interval: Intervalable): List<Intervalable> =
        checkForOverlaps(interval, Direction.RIGHT)

    /** Scans this node's intervals in the given [direction] for overlaps with [interval]. */
    private fun checkForOverlaps(
        interval: Intervalable,
        direction: Direction,
    ): List<Intervalable> {
        val overlaps = LinkedList<Intervalable>()

        this.intervals.forEach {
            when (direction) {
                Direction.LEFT  -> {
                    if (it.start <= interval.end) {
                        overlaps.add(it)
                    }
                }

                Direction.RIGHT -> {
                    if (it.end >= interval.start) {
                        overlaps.add(it)
                    }
                }
            }
        }
        return overlaps
    }

    /** Delegates overlap search to [node], returning an empty list when [node] is null. */
    private fun findOverlappingRanges(
        node: IntervalNode?,
        interval: Intervalable,
    ): List<Intervalable> = node?.findOverlaps(interval) ?: emptyList()

    /** Returns `true` if [other] is an [IntervalNode] with equal [left], [right], [median], and [intervals]. */
    override fun equalProperties(other: Any): Boolean =
        other is IntervalNode &&
                left == other.left &&
                right == other.right &&
                median == other.median &&
                intervals == other.intervals

    override fun hashCode(): Int = hashOf(left, right, median, intervals)
}
