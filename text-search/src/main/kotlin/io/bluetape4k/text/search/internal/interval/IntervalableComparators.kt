package io.bluetape4k.text.search.internal.interval

/**
 * [Intervalable] 객체 정렬에 사용하는 comparator 모음입니다.
 *
 * ## 동작 계약
 * - 크기가 같으면 [Intervalable.start] 위치로 tie-break합니다.
 * - Comparator는 입력 객체를 변경하지 않습니다.
 */
internal object IntervalableComparators {
    /**
     * 구간을 size 오름차순으로 정렬합니다. Size가 같으면 [Intervalable.start] 오름차순으로 정렬합니다.
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
     * 구간을 size 내림차순으로 정렬합니다. Size가 같으면 [Intervalable.start] 오름차순으로 정렬합니다.
     */
    val ReverseSizeComparator: Comparator<Intervalable> =
        Comparator { o1, o2 ->
            var comparison = o2.size - o1.size
            if (comparison == 0) {
                comparison = o1.start - o2.start
            }
            comparison
        }

    /** 구간을 [Intervalable.start] 위치 오름차순으로 정렬합니다. */
    val PositionComparator: Comparator<Intervalable> =
        Comparator { o1, o2 ->
            o1.start - o2.start
        }
}
