package io.bluetape4k.text.search.internal

import io.bluetape4k.text.search.internal.interval.Interval

/**
 * An Aho-Corasick emit: an [Interval] with an associated matched keyword.
 *
 * ```kotlin
 * val emit = Emit(2, 5, "hers")
 * // emit.size == 4
 * ```
 *
 * @property start inclusive start offset
 * @property end inclusive end offset
 * @property keyword matched keyword, or `null` if not applicable
 */
internal class Emit(
    override val start: Int,
    override val end: Int,
    val keyword: String? = null,
): Interval(start, end) {

    override fun toString(): String = super.toString() + "=${keyword ?: "<null>"}"

}
