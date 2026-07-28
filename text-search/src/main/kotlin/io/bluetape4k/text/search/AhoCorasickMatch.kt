package io.bluetape4k.text.search

import java.io.Serializable

/**
 * [AhoCorasickAutomaton]이 만든 단일 키워드 match 결과입니다.
 *
 * `kotlin.text.MatchResult`와 이름 충돌을 피하기 위해 `AhoCorasickMatch`라고 부릅니다.
 *
 * @param V match된 키워드에 연결된 공변 값 타입입니다.
 * @property start 원본 문자열의 inclusive start offset입니다.
 * @property end 원본 문자열의 inclusive end offset입니다.
 * @property keyword match된 키워드입니다. 정규화가 켜져 있으면 정규화된 형태입니다.
 * @property value 키워드에 연결된 값입니다.
 */
data class AhoCorasickMatch<out V>(
    val start: Int,
    val end: Int,
    val keyword: String,
    val value: V,
) : Serializable {

    companion object {
        private const val serialVersionUID: Long = 1L
    }

    /** Match 길이입니다. 계산식은 `end - start + 1`입니다. */
    val length: Int get() = end - start + 1
}
