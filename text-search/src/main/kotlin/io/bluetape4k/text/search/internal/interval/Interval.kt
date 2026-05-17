package io.bluetape4k.text.search.internal.interval

import io.bluetape4k.AbstractValueObject
import io.bluetape4k.support.hashOf

/**
 * Basic interval implementation identified by inclusive [start] and [end] indices.
 *
 * ## Behavior / Contract
 * - When [start] > [end], the interval is considered empty ([isEmpty] returns `true`).
 * - [compareTo] sorts by `start` then `end`.
 * - Value equality compares only [start] and [end].
 *
 * ```kotlin
 * val interval = Interval(2, 5)
 * // interval.size == 4
 * ```
 */
internal open class Interval(
    override val start: Int,
    override val end: Int,
): AbstractValueObject(), Intervalable {

    companion object {
        @JvmField
        val EMPTY = Interval(1, 0)
    }

    /** Returns `true` if this interval is empty ([start] > [end]). */
    val isEmpty: Boolean get() = start > end

    /** Returns `true` if this interval overlaps with [other]. */
    fun overlapsWith(other: Interval): Boolean {
        return start < other.end && end >= other.start
    }

    /** Returns `true` if [point] falls within this interval (inclusive). */
    fun overlapsWith(point: Int): Boolean {
        return point in start..end
    }

    override fun compareTo(other: Intervalable): Int {
        var comparison = start - other.start
        if (comparison == 0) {
            comparison = end - other.end
        }
        return comparison
    }

    override fun equalProperties(other: Any): Boolean =
        other is Intervalable &&
                start == other.start &&
                end == other.end

    override fun equals(other: Any?): Boolean = other?.let { equalProperties(it) } ?: false
    override fun hashCode(): Int = if (isEmpty) -1 else hashOf(start, end)
    override fun toString(): String = "Interval($start:$end)"
}
