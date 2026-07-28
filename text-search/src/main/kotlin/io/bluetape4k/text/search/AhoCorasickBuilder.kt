package io.bluetape4k.text.search

import io.bluetape4k.support.requireNotBlank

/**
 * [AhoCorasickBuilder]에서 암시적인 바깥 scope receiver 접근을 막는 DSL marker annotation입니다.
 */
@DslMarker
annotation class AhoCorasickDsl

/**
 * [AhoCorasickAutomaton]을 구성하는 DSL builder입니다. [ahoCorasick] top-level function으로 사용합니다.
 *
 * ```kotlin
 * val automaton = ahoCorasick<String> {
 *     ignoreCase = true
 *     allowOverlaps = false
 *     keyword("apple", "APPLE")
 *     keyword("banana", "BANANA")
 * }
 * ```
 *
 * @param V 각 키워드에 연결된 값 타입입니다.
 */
@AhoCorasickDsl
class AhoCorasickBuilder<V> internal constructor() {

    /** 매칭 중 대소문자를 무시할지 여부입니다. 기본값은 `false`입니다. */
    var ignoreCase: Boolean = false

    /** 겹치는 match를 허용할지 여부입니다. 기본값은 `true`입니다. */
    var allowOverlaps: Boolean = true

    /** 단어 경계 감지 모드입니다. 기본값은 [WordBoundary.NONE]입니다. */
    var wordBoundary: WordBoundary = WordBoundary.NONE

    /** 매칭 전에 적용할 유니코드 정규화 형식입니다. 기본값은 [NormalizationForm.NONE]입니다. */
    var normalization: NormalizationForm = NormalizationForm.NONE

    /** 첫 match를 찾은 뒤 중단할지 여부입니다. 기본값은 `false`입니다. */
    var stopOnFirstMatch: Boolean = false

    private val entries = mutableMapOf<String, V>()

    /**
     * 키워드와 연결 값을 등록합니다.
     *
     * @param keyword 등록할 키워드입니다. Blank이면 안 됩니다.
     * @param value 키워드에 연결할 값입니다.
     * @throws IllegalArgumentException [keyword]가 blank이면 던집니다.
     */
    fun keyword(keyword: String, value: V) {
        keyword.requireNotBlank("keyword")
        entries[keyword] = value
    }

    /**
     * 여러 키워드와 값 pair를 등록합니다.
     *
     * @param pairs 키워드를 값에 연결한 pair 목록입니다. 각 key는 blank이면 안 됩니다.
     */
    fun keywords(vararg pairs: Pair<String, V>) {
        pairs.forEach { (k, v) -> keyword(k, v) }
    }

    /**
     * Map에서 여러 키워드와 값 pair를 등록합니다.
     *
     * @param map 키워드를 값에 매핑한 map입니다. 각 key는 blank이면 안 됩니다.
     */
    fun keywords(map: Map<String, V>) {
        map.forEach { (k, v) -> keyword(k, v) }
    }

    /**
     * 현재 설정으로 불변 [AhoCorasickAutomaton]을 생성합니다.
     *
     * @return 생성이 끝난 불변 [AhoCorasickAutomaton]입니다.
     */
    internal fun build(): AhoCorasickAutomaton<V> {
        val opts = SearchOptions(
            ignoreCase = ignoreCase,
            allowOverlaps = allowOverlaps,
            wordBoundary = wordBoundary,
            normalization = normalization,
            stopOnFirstMatch = stopOnFirstMatch,
        )
        val builder = AhoCorasickAutomaton.builder<V>()
        entries.forEach { (k, v) -> builder.add(k, v) }
        return builder.options(opts).build()
    }
}

/**
 * DSL builder block을 사용해 [AhoCorasickAutomaton]을 만듭니다.
 *
 * ```kotlin
 * val automaton = ahoCorasick<Int> {
 *     ignoreCase = true
 *     keyword("foo", 1)
 *     keyword("bar", 2)
 * }
 * ```
 *
 * @param V 각 키워드에 연결된 값 타입입니다.
 * @param block [AhoCorasickBuilder]에 적용할 설정 block입니다.
 * @return DSL 설정으로 생성한 [AhoCorasickAutomaton]입니다.
 */
fun <V> ahoCorasick(block: AhoCorasickBuilder<V>.() -> Unit): AhoCorasickAutomaton<V> =
    AhoCorasickBuilder<V>().apply(block).build()

/**
 * 키워드 문자열들로 [AhoCorasickAutomaton]을 만들고 각 키워드를 자기 자신에 매핑합니다.
 *
 * ```kotlin
 * val automaton = ahoCorasickOf("apple", "banana", "cherry")
 * ```
 *
 * @param keywords 등록할 키워드 목록입니다. 각 값은 blank이면 안 됩니다.
 * @param options 검색 옵션입니다. 기본값은 [SearchOptions]입니다.
 * @return 키워드를 자기 자신에 매핑하는 [AhoCorasickAutomaton]입니다.
 */
fun ahoCorasickOf(
    vararg keywords: String,
    options: SearchOptions = SearchOptions(),
): AhoCorasickAutomaton<String> =
    ahoCorasickOf(keywords.toList(), options)

/**
 * 키워드 collection으로 [AhoCorasickAutomaton]을 만들고 각 키워드를 자기 자신에 매핑합니다.
 *
 * ```kotlin
 * val automaton = ahoCorasickOf(listOf("apple", "banana"), SearchOptions(ignoreCase = true))
 * ```
 *
 * @param keywords 등록할 키워드 collection입니다. 각 값은 blank이면 안 됩니다.
 * @param options 검색 옵션입니다. 기본값은 [SearchOptions]입니다.
 * @return 키워드를 자기 자신에 매핑하는 [AhoCorasickAutomaton]입니다.
 */
fun ahoCorasickOf(
    keywords: Collection<String>,
    options: SearchOptions = SearchOptions(),
): AhoCorasickAutomaton<String> {
    val builder = AhoCorasickAutomaton.builder<String>()
    keywords.forEach { builder.add(it, it) }
    return builder.options(options).build()
}
