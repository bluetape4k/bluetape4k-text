package io.bluetape4k.text.search.internal.interval

import io.bluetape4k.AbstractValueObject
import io.bluetape4k.support.hashOf

/**
 * Inclusive [start]와 [end] index로 표현하는 기본 구간 구현입니다.
 *
 * ## 동작 계약
 * - [start] > [end]이면 빈 구간으로 간주합니다. 이때 [isEmpty]는 `true`입니다.
 * - [compareTo]는 `start`, 그다음 `end` 순서로 정렬합니다.
 * - 값 동등성은 [start]와 [end]만 비교합니다.
 *
 * ```kotlin
 * val interval = Interval(2, 5)
 * // interval.size == 4
 * ```
 * @property start inclusive 시작 index입니다.
 * @property end inclusive 종료 index입니다.
 */
internal open class Interval(
    override val start: Int,
    override val end: Int,
): AbstractValueObject(), Intervalable {

    companion object {
        @JvmField
        val EMPTY = Interval(1, 0)
    }

    /** 이 구간이 비어 있으면, 즉 [start] > [end]이면 `true`입니다. */
    val isEmpty: Boolean get() = start > end

    /** 이 구간이 [other]와 겹치면 `true`를 반환합니다. */
    fun overlapsWith(other: Interval): Boolean {
        return start < other.end && end >= other.start
    }

    /** [point]가 이 inclusive 구간 안에 있으면 `true`를 반환합니다. */
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
