package io.bluetape4k.text.search.internal.interval

import io.bluetape4k.ValueObject

/**
 * Contract for an object with inclusive [start] and [end] indices.
 *
 * ## Behavior / Contract
 * - Both [start] and [end] are inclusive.
 * - [size] is computed as `end - start + 1`.
 *
 * ```kotlin
 * val i: Intervalable = Interval(2, 4)
 * // i.size == 3
 * ```
 */
internal interface Intervalable: Comparable<Intervalable>, ValueObject {

    val start: Int
    val end: Int

    val size: Int get() = end - start + 1
}
