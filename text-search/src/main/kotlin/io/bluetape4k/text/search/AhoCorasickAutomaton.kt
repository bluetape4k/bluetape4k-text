package io.bluetape4k.text.search

import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.text.search.internal.InternalTrieConfig
import io.bluetape4k.text.search.internal.EmitHandler
import io.bluetape4k.text.search.internal.OffsetMapping
import io.bluetape4k.text.search.internal.TrieCore
import io.bluetape4k.text.search.internal.applyPipeline

/**
 * Immutable, thread-safe Aho-Corasick automaton that maps keywords to associated values.
 *
 * Build via the [Builder] (or the [ahoCorasick] DSL). After [Builder.build] returns, the
 * automaton is immutable and safe for concurrent search.
 *
 * **Note**: `AhoCorasickAutomaton<V>` does not implement `Serializable` because the internal
 * `TrieCore` state (failure transitions) contains cyclic references.
 *
 * ```kotlin
 * val automaton = AhoCorasickAutomaton.builder<String>()
 *     .add("apple", "A")
 *     .add("banana", "B")
 *     .options(SearchOptions(ignoreCase = true))
 *     .build()
 *
 * val matches = automaton.parseText("I like APPLE and Banana.")
 * // matches: [(start=7, end=11, keyword="apple", value="A"), ...]
 * ```
 *
 * @param V type of value associated with each keyword
 */
class AhoCorasickAutomaton<V> internal constructor(
    private val core: TrieCore,
    private val values: Map<String, V>,
    internal val options: SearchOptions,
) {

    companion object: KLogging() {
        /**
         * Creates a new [Builder] instance.
         *
         * @param V type of value associated with each keyword
         */
        @JvmStatic
        fun <V> builder(): Builder<V> = Builder()
    }

    /**
     * Searches [text] for all registered keywords and returns their match results.
     *
     * When [SearchOptions.ignoreCase] is `true`, text is lowercased before searching.
     * When [SearchOptions.stopOnFirstMatch] is `true`, only the first match is returned.
     * Keywords in results reflect the normalized (and optionally lowercased) form.
     *
     * @param text input text to search
     * @return list of matches sorted by start position ascending
     */
    fun parseText(text: CharSequence): List<AhoCorasickMatch<V>> {
        if (text.isEmpty() || values.isEmpty()) {
            return emptyList()
        }

        // 1. 유니코드 정규화 + offset mapping 구축 (NONE이면 mapping은 null)
        val (normalizedText, mapping) = OffsetMapping.build(text, options.normalization)

        // 2. ignoreCase 적용 — Locale.ROOT 기준 소문자 변환
        val processedText: CharSequence = if (options.ignoreCase) {
            normalizedText.lowercaseCharByChar()
        } else {
            normalizedText
        }

        val emits = core.parseText(processedText)
        if (emits.isEmpty()) {
            return emptyList()
        }

        val matches = ArrayList<AhoCorasickMatch<V>>(emits.size)
        for (emit in emits) {
            val keyword = emit.keyword ?: continue
            val value = values[keyword] ?: continue
            // 정규화된 offset → 원본 offset 복원
            val origStart = mapping?.toOriginal(emit.start) ?: emit.start
            val origEnd = mapping?.toOriginalEndInclusive(emit.end) ?: emit.end
            matches.add(
                AhoCorasickMatch(
                    start = origStart,
                    end = origEnd,
                    keyword = keyword,
                    value = value,
                )
            )
            if (options.stopOnFirstMatch) {
                break
            }
        }
        return matches
    }

    /**
     * Traverses the trie and invokes [onMatch] as each raw match is found.
     *
     * This path intentionally bypasses post-processing filters such as overlap removal and word-boundary
     * filtering. Callers that require those filters should use [parseText].
     */
    internal suspend fun forEachRawMatch(
        text: CharSequence,
        ignoreStopOnFirstMatch: Boolean,
        onMatch: suspend (AhoCorasickMatch<V>) -> Unit,
    ) {
        if (text.isEmpty() || values.isEmpty()) {
            return
        }

        val (normalizedText, mapping) = OffsetMapping.build(text, options.normalization)
        val processedText: CharSequence = if (options.ignoreCase) {
            normalizedText.lowercaseCharByChar()
        } else {
            normalizedText
        }

        core.runParseTextSuspending(
            processedText,
            { emit ->
                val keyword = emit.keyword ?: return@runParseTextSuspending true
                val value = values[keyword] ?: return@runParseTextSuspending true
                val origStart = mapping?.toOriginal(emit.start) ?: emit.start
                val origEnd = mapping?.toOriginalEndInclusive(emit.end) ?: emit.end
                onMatch(
                    AhoCorasickMatch(
                        start = origStart,
                        end = origEnd,
                        keyword = keyword,
                        value = value,
                    )
                )
                true
            },
            stopOnHit = !ignoreStopOnFirstMatch && options.stopOnFirstMatch,
        )
    }

    /**
     * Returns the leftmost-longest match in [text] (earliest start; longest on tie), or `null` if none.
     *
     * @param text input text to search
     */
    fun firstMatch(text: CharSequence): AhoCorasickMatch<V>? {
        val matches = parseText(text)
        if (matches.isEmpty()) {
            return null
        }
        // leftmost (start ASC) → longer wins (length DESC)
        return matches.minWithOrNull(
            compareBy<AhoCorasickMatch<V>> { it.start }.thenByDescending { it.length }
        )
    }

    /**
     * Returns `true` if [text] contains at least one registered keyword match.
     *
     * More efficient than [parseText] when only existence needs to be checked — stops at the first match.
     *
     * @param text input text to search
     */
    fun containsMatch(text: CharSequence): Boolean {
        if (text.isEmpty() || values.isEmpty()) return false
        val (normalizedText, _) = OffsetMapping.build(text, options.normalization)
        val processedText: CharSequence = if (options.ignoreCase) {
            normalizedText.lowercaseCharByChar()
        } else {
            normalizedText
        }
        return core.containsMatch(processedText)
    }

    /**
     * Splits [text] into a sequence of [SearchToken.Match] and [SearchToken.Fragment] tokens.
     *
     * Non-matching spans between matches, and any trailing text after the last match, are emitted
     * as [SearchToken.Fragment]. Empty input returns an empty list.
     *
     * @param text input text to tokenize
     * @return list of match/fragment tokens in text order
     */
    fun tokenize(text: CharSequence): List<SearchToken<V>> {
        if (text.isEmpty()) {
            return emptyList()
        }

        val matches = parseText(text)
        if (matches.isEmpty()) {
            return listOf(SearchToken.Fragment(text.toString()))
        }

        val original = text.toString()
        val tokens = ArrayList<SearchToken<V>>(matches.size * 2 + 1)
        var lastEnd = 0  // exclusive cursor in `original`

        // allowOverlaps=true일 때도 tokenize는 비겹침 시퀀스를 생성해야 하므로
        // replaceAll과 동일하게 start ASC, length DESC 정렬 후 겹침 skip
        val sorted = matches.sortedWith(
            compareBy<AhoCorasickMatch<V>> { it.start }.thenByDescending { it.length }
        )

        for (match in sorted) {
            if (match.start < lastEnd) continue  // 앞 매치와 겹치는 경우 skip
            // 비매치 구간 (lastEnd ~ match.start)
            if (match.start > lastEnd) {
                tokens.add(SearchToken.Fragment(original.substring(lastEnd, match.start)))
            }
            // 매치 구간 (match.start ~ match.end inclusive)
            val matchText = original.substring(match.start, match.end + 1)
            tokens.add(SearchToken.Match(matchText, match))
            lastEnd = match.end + 1
        }

        // 꼬리 처리
        if (lastEnd < original.length) {
            tokens.add(SearchToken.Fragment(original.substring(lastEnd)))
        }
        return tokens
    }

    /**
     * Replaces every matched keyword in [text] with the result of [transform] and returns the new string.
     *
     * Matches are processed in start-ASC / length-DESC order. Overlapping matches (relative to the
     * previous replacement cursor) are skipped even when [SearchOptions.allowOverlaps] is `true`.
     * Returns [text].toString() unchanged when there are no matches.
     *
     * @param text input text to process
     * @param transform function that maps a match to its replacement string
     */
    fun replaceAll(
        text: CharSequence,
        transform: (AhoCorasickMatch<V>) -> CharSequence,
    ): String {
        val matches = parseText(text)
        if (matches.isEmpty()) {
            return text.toString()
        }

        val original = text.toString()
        val sorted = matches.sortedWith(
            compareBy<AhoCorasickMatch<V>> { it.start }.thenByDescending { it.length }
        )

        val sb = StringBuilder(original.length)
        var cursor = 0  // exclusive cursor

        for (match in sorted) {
            // allowOverlaps=true 인 경우에도 치환은 비겹침이어야 함 — 이전 매치와 겹치면 skip
            if (match.start < cursor) {
                continue
            }
            // 비매치 prefix
            if (match.start > cursor) {
                sb.append(original, cursor, match.start)
            }
            sb.append(transform(match))
            cursor = match.end + 1
        }

        // 꼬리 처리
        if (cursor < original.length) {
            sb.append(original, cursor, original.length)
        }
        return sb.toString()
    }

    /**
     * Step-by-step builder for [AhoCorasickAutomaton].
     *
     * ```kotlin
     * val automaton = AhoCorasickAutomaton.builder<Int>()
     *     .add("foo", 1)
     *     .add("bar", 2)
     *     .options(SearchOptions(ignoreCase = true, allowOverlaps = false))
     *     .build()
     * ```
     *
     * @param V type of value associated with each keyword
     */
    class Builder<V> {
        companion object: KLogging()

        private val entries: MutableMap<String, V> = mutableMapOf()
        private var opts: SearchOptions = SearchOptions()

        /**
         * Registers a keyword and its associated value.
         *
         * @param keyword keyword to register (must not be blank)
         * @param value value associated with the keyword
         * @return this builder (for chaining)
         */
        fun add(keyword: String, value: V): Builder<V> = apply {
            keyword.requireNotBlank("keyword")
            entries[keyword] = value
        }

        /**
         * Registers multiple keyword/value pairs from a map.
         *
         * @param map keyword-to-value map (each key must not be blank)
         * @return this builder (for chaining)
         */
        fun addAll(map: Map<String, V>): Builder<V> = apply {
            map.forEach { (keyword, value) -> add(keyword, value) }
        }

        /**
         * Sets the search options.
         *
         * @param options search options to apply
         * @return this builder (for chaining)
         */
        fun options(options: SearchOptions): Builder<V> = apply {
            this.opts = options
        }

        /**
         * Builds an immutable [AhoCorasickAutomaton] from the registered keywords and options.
         *
         * When [SearchOptions.ignoreCase] is `true`, all keywords are lowercased before being
         * added to the trie so that search-time normalization matches build-time normalization.
         */
        fun build(): AhoCorasickAutomaton<V> {
            // 검색 시점과 동일한 파이프라인(NFC/NFKC 정규화 + ignoreCase)을 키워드에도 적용해야
            // 매치 일관성이 보장된다.
            val normalizedValues = HashMap<String, V>(entries.size)
            entries.forEach { (keyword, value) ->
                val normalized = applyPipeline(keyword, opts)
                normalizedValues[normalized] = value
            }

            val coreBuilder = TrieCore.builder()
            normalizedValues.keys.forEach { coreBuilder.addKeyword(it) }

            // SearchOptions → TrieCore Builder 옵션 매핑
            if (opts.ignoreCase) {
                coreBuilder.ignoreCase()
            }
            if (!opts.allowOverlaps) {
                coreBuilder.ignoreOverlaps()
            }
            when (opts.wordBoundary) {
                WordBoundary.NONE -> { /* default */ }
                WordBoundary.LATIN_ALPHA -> coreBuilder.onlyWholeWords()
                WordBoundary.WHITESPACE_SEPARATED -> coreBuilder.onlyWholeWordsWhiteSpaceSeparated()
            }
            if (opts.stopOnFirstMatch) {
                coreBuilder.stopOnHit()
            }

            val core = coreBuilder.build()
            return AhoCorasickAutomaton(core, normalizedValues.toMap(), opts)
        }
    }
}

/**
 * Lowercase a string char-by-char using [Char.lowercaseChar].
 *
 * Unlike [String.lowercase] with [java.util.Locale.ROOT], `Char.lowercaseChar()` always returns
 * a single `Char` (BMP-safe), so the resulting string is guaranteed to have the same length as
 * the receiver. This prevents offset mismatches when `ignoreCase = true` is used together with
 * [OffsetMapping]-based normalization.
 */
private fun String.lowercaseCharByChar(): String = buildString(length) {
    for (c in this@lowercaseCharByChar) append(c.lowercaseChar())
}
