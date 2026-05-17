package io.bluetape4k.text.search

import io.bluetape4k.support.requireNotBlank

/**
 * DSL marker annotation for [AhoCorasickBuilder] to prevent implicit outer-scope receiver access.
 */
@DslMarker
annotation class AhoCorasickDsl

/**
 * DSL builder for [AhoCorasickAutomaton]. Use via the [ahoCorasick] top-level function.
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
 * @param V type of value associated with each keyword
 */
@AhoCorasickDsl
class AhoCorasickBuilder<V> internal constructor() {

    /** Whether to ignore case during matching (default: `false`). */
    var ignoreCase: Boolean = false

    /** Whether overlapping matches are allowed (default: `true`). */
    var allowOverlaps: Boolean = true

    /** Word boundary detection mode (default: [WordBoundary.NONE]). */
    var wordBoundary: WordBoundary = WordBoundary.NONE

    /** Unicode normalization form applied before matching (default: [NormalizationForm.NONE]). */
    var normalization: NormalizationForm = NormalizationForm.NONE

    /** Whether to stop after the first match is found (default: `false`). */
    var stopOnFirstMatch: Boolean = false

    private val entries = mutableMapOf<String, V>()

    /**
     * Registers a keyword and its associated value.
     *
     * @param keyword keyword to register (must not be blank)
     * @param value value associated with the keyword
     * @throws IllegalArgumentException if [keyword] is blank
     */
    fun keyword(keyword: String, value: V) {
        keyword.requireNotBlank("keyword")
        entries[keyword] = value
    }

    /**
     * Registers multiple keyword/value pairs.
     *
     * @param pairs keyword-to-value pairs (each key must not be blank)
     */
    fun keywords(vararg pairs: Pair<String, V>) {
        pairs.forEach { (k, v) -> keyword(k, v) }
    }

    /**
     * Registers multiple keyword/value pairs from a map.
     *
     * @param map keyword-to-value map (each key must not be blank)
     */
    fun keywords(map: Map<String, V>) {
        map.forEach { (k, v) -> keyword(k, v) }
    }

    /**
     * Builds an immutable [AhoCorasickAutomaton] from the current configuration.
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
 * Creates an [AhoCorasickAutomaton] using a DSL builder block.
 *
 * ```kotlin
 * val automaton = ahoCorasick<Int> {
 *     ignoreCase = true
 *     keyword("foo", 1)
 *     keyword("bar", 2)
 * }
 * ```
 *
 * @param V type of value associated with each keyword
 * @param block configuration block applied to [AhoCorasickBuilder]
 */
fun <V> ahoCorasick(block: AhoCorasickBuilder<V>.() -> Unit): AhoCorasickAutomaton<V> =
    AhoCorasickBuilder<V>().apply(block).build()

/**
 * Creates an [AhoCorasickAutomaton] from keyword strings, mapping each keyword to itself.
 *
 * ```kotlin
 * val automaton = ahoCorasickOf("apple", "banana", "cherry")
 * ```
 *
 * @param keywords keywords to register (each must not be blank)
 * @param options search options (default: [SearchOptions])
 */
fun ahoCorasickOf(
    vararg keywords: String,
    options: SearchOptions = SearchOptions(),
): AhoCorasickAutomaton<String> =
    ahoCorasickOf(keywords.toList(), options)

/**
 * Creates an [AhoCorasickAutomaton] from a keyword collection, mapping each keyword to itself.
 *
 * ```kotlin
 * val automaton = ahoCorasickOf(listOf("apple", "banana"), SearchOptions(ignoreCase = true))
 * ```
 *
 * @param keywords keywords to register (each must not be blank)
 * @param options search options (default: [SearchOptions])
 */
fun ahoCorasickOf(
    keywords: Collection<String>,
    options: SearchOptions = SearchOptions(),
): AhoCorasickAutomaton<String> {
    val builder = AhoCorasickAutomaton.builder<String>()
    keywords.forEach { builder.add(it, it) }
    return builder.options(options).build()
}
