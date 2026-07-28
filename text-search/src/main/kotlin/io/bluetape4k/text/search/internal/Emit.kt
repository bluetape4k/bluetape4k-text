package io.bluetape4k.text.search.internal

import io.bluetape4k.text.search.internal.interval.Interval

/**
 * Aho-Corasick emit입니다. Match된 키워드를 연결한 [Interval]입니다.
 *
 * ```kotlin
 * val emit = Emit(2, 5, "hers")
 * // emit.size == 4
 * ```
 *
 * @property start inclusive start offset입니다.
 * @property end inclusive end offset입니다.
 * @property keyword match된 키워드입니다. 해당 값이 없으면 `null`입니다.
 */
internal class Emit(
    override val start: Int,
    override val end: Int,
    val keyword: String? = null,
): Interval(start, end) {

    override fun toString(): String = super.toString() + "=${keyword ?: "<null>"}"

}
