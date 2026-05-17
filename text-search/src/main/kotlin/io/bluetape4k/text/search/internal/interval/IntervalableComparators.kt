package io.bluetape4k.text.search.internal.interval

/**
 * Comparators for sorting [Intervalable] objects.
 *
 * ## Behavior / Contract
 * - When sizes are equal, [start] position is used as tie-breaker.
 * - Comparators do not mutate input objects.
 */
internal object IntervalableComparators {
    /**
     * Sorts intervals by size ascending; ties broken by [Intervalable.start] ascending.
     */
    val SizeComparator: Comparator<Intervalable> =
        Comparator { o1, o2 ->
            var comparison = o1.size - o2.size
            if (comparison == 0) {
                comparison = o1.start - o2.start
            }
            comparison
        }

    /**
     * Sorts intervals by size descending; ties broken by [Intervalable.start] ascending.
     */
    val ReverseSizeComparator: Comparator<Intervalable> =
        Comparator { o1, o2 ->
            var comparison = o2.size - o1.size
            if (comparison == 0) {
                comparison = o1.start - o2.start
            }
            comparison
        }

    /** Sorts intervals by [Intervalable.start] position ascending. */
    val PositionComparator: Comparator<Intervalable> =
        Comparator { o1, o2 ->
            o1.start - o2.start
        }
}
