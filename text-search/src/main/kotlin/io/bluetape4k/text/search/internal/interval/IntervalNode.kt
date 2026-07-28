package io.bluetape4k.text.search.internal.interval

import io.bluetape4k.AbstractValueObject
import io.bluetape4k.support.hashOf
import java.util.*

/**
 * [median] 값을 기준으로 구간을 나누는 interval tree node입니다.
 *
 * @param inputs 이 node와 하위 node에 저장할 구간 collection입니다.
 * @see IntervalTree
 */
internal class IntervalNode(
    inputs: Collection<Intervalable>,
): AbstractValueObject() {
    /** Tree 순회 방향입니다. */
    enum class Direction {
        LEFT,
        RIGHT
    }

    /** 왼쪽 자식 node입니다. */
    var left: IntervalNode? = null

    /** 오른쪽 자식 node입니다. */
    var right: IntervalNode? = null

    /** 이 node에 저장된 구간입니다. [median]을 가로지르는 구간이 여기에 남습니다. */
    val intervals = LinkedList<Intervalable>()

    /** 자식 구간을 왼쪽과 오른쪽으로 나누는 기준값입니다. */
    val median: Int

    init {
        median = determineMedian(inputs)
        buildTree(inputs)
    }

    /**
     * [inputs] collection의 median을 `(minStart + maxEnd) / 2`로 계산합니다.
     *
     * @param inputs median을 계산할 구간 collection입니다.
     * @return interval tree 분할에 사용할 median 값입니다.
     */
    private fun determineMedian(inputs: Collection<Intervalable>): Int {
        val start = inputs.minOfOrNull { it.start } ?: 0
        val end = inputs.maxOfOrNull { it.end } ?: 0
        return (start + end) / 2
    }

    /**
     * [median]을 기준으로 [inputs]를 왼쪽 자식, 오른쪽 자식, 현재 node의 [intervals]로 나눕니다.
     *
     * @param inputs 분할할 구간 collection입니다.
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
     * 이 subtree에서 [interval]과 겹치는 모든 구간을 찾아 [destination]에 추가합니다.
     *
     * @param interval 겹침을 검사할 기준 구간입니다.
     * @param destination 찾은 구간을 누적할 mutable list입니다. 기본값은 새 list입니다.
     * @return [destination]에 겹치는 구간을 추가한 list입니다.
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

    /** [interval] 자신을 제외하고 [newOverlaps]를 [overlaps]에 추가합니다. */
    private fun addToOverlaps(
        interval: Intervalable,
        overlaps: MutableList<Intervalable>,
        newOverlaps: List<Intervalable>,
    ) {
        overlaps.addAll(newOverlaps.filter { it != interval })
    }

    /** 현재 node의 구간 중 왼쪽 방향에서 [interval]과 겹치는 구간을 검사합니다. */
    private fun checkForOverlapsToLeft(interval: Intervalable): List<Intervalable> =
        checkForOverlaps(interval, Direction.LEFT)

    /** 현재 node의 구간 중 오른쪽 방향에서 [interval]과 겹치는 구간을 검사합니다. */
    private fun checkForOverlapsToRight(interval: Intervalable): List<Intervalable> =
        checkForOverlaps(interval, Direction.RIGHT)

    /** 현재 node의 구간을 [direction] 방향 기준으로 훑으며 [interval]과 겹치는 구간을 찾습니다. */
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

    /** [node]에 겹침 검색을 위임합니다. [node]가 `null`이면 빈 list를 반환합니다. */
    private fun findOverlappingRanges(
        node: IntervalNode?,
        interval: Intervalable,
    ): List<Intervalable> = node?.findOverlaps(interval) ?: emptyList()

    /** [other]가 [left], [right], [median], [intervals]가 같은 [IntervalNode]이면 `true`를 반환합니다. */
    override fun equalProperties(other: Any): Boolean =
        other is IntervalNode &&
                left == other.left &&
                right == other.right &&
                median == other.median &&
                intervals == other.intervals

    override fun hashCode(): Int = hashOf(left, right, median, intervals)
}
