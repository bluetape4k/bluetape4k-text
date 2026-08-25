package io.bluetape4k.text.search.internal.interval

import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeTrue
import org.junit.jupiter.api.Test

class IntervalTest {

    @Test
    fun `non-empty interval overlaps itself`() {
        Interval(2, 5).overlapsWith(Interval(2, 5)).shouldBeTrue()
    }

    @Test
    fun `inclusive boundary overlaps in both directions`() {
        val left = Interval(0, 2)
        val right = Interval(2, 4)

        left.overlapsWith(right).shouldBeTrue()
        right.overlapsWith(left).shouldBeTrue()
    }

    @Test
    fun `disjoint intervals do not overlap`() {
        Interval(0, 1).overlapsWith(Interval(2, 3)).shouldBeFalse()
    }

    @Test
    fun `empty interval does not overlap a non-empty interval`() {
        Interval(3, 2).overlapsWith(Interval(0, 5)).shouldBeFalse()
    }

    @Test
    fun `empty interval does not overlap itself`() {
        Interval.EMPTY.overlapsWith(Interval.EMPTY).shouldBeFalse()
    }

    @Test
    fun `point overlap includes both inclusive boundaries`() {
        val interval = Interval(2, 5)

        interval.overlapsWith(2).shouldBeTrue()
        interval.overlapsWith(5).shouldBeTrue()
    }

    @Test
    fun `empty interval does not overlap a point`() {
        Interval(3, 2).overlapsWith(3).shouldBeFalse()
    }
}
