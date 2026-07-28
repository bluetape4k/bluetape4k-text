package io.bluetape4k.text.search

import java.io.Serializable

/**
 * [io.bluetape4k.text.search.AhoCorasickAutomaton.tokenize]가 만드는 token입니다.
 *
 * 입력 문자열은 키워드 match 구간([Match])과 비매치 구간([Fragment])으로 나뉩니다.
 *
 * @param V match된 키워드에 연결된 공변 값 타입입니다.
 */
sealed interface SearchToken<out V> : Serializable {

    /**
     * 키워드 match 구간입니다.
     *
     * @property text 원본 문자열에서 추출한 match substring입니다.
     * @property match 자세한 match 정보([AhoCorasickMatch])입니다.
     */
    data class Match<out V>(
        val text: String,
        val match: AhoCorasickMatch<V>,
    ) : SearchToken<V> {
        companion object {
            private const val serialVersionUID: Long = 1L
        }
    }

    /**
     * 비매치 구간입니다. 키워드 match 사이의 일반 문자열을 나타냅니다.
     *
     * @property text 원본 문자열에서 추출한 비매치 substring입니다.
     */
    data class Fragment(
        val text: String,
    ) : SearchToken<Nothing> {
        companion object {
            private const val serialVersionUID: Long = 1L
        }
    }
}
