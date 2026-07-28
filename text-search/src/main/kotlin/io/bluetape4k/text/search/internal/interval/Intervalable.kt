package io.bluetape4k.text.search.internal.interval

import io.bluetape4k.ValueObject

/**
 * Inclusive [start]와 [end] index를 가진 객체의 계약입니다.
 *
 * ## 동작 계약
 * - [start]와 [end]는 모두 inclusive입니다.
 * - [size]는 `end - start + 1`로 계산합니다.
 *
 * ```kotlin
 * val i: Intervalable = Interval(2, 4)
 * // i.size == 3
 * ```
 */
internal interface Intervalable: Comparable<Intervalable>, ValueObject {

    val start: Int
    val end: Int

    /** Inclusive 구간 길이입니다. */
    val size: Int get() = end - start + 1
}
