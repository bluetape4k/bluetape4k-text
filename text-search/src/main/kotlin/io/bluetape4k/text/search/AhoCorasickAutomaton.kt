package io.bluetape4k.text.search

import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.text.search.internal.CaseFoldedText
import io.bluetape4k.text.search.internal.Emit
import io.bluetape4k.text.search.internal.InternalTrieConfig
import io.bluetape4k.text.search.internal.OffsetMapping
import io.bluetape4k.text.search.internal.TrieCore
import io.bluetape4k.text.search.internal.applyPipeline
import io.bluetape4k.text.search.internal.lowercaseWithMapping

/**
 * 키워드를 연결 값에 매핑하는 불변, 스레드 안전 Aho-Corasick automaton입니다.
 *
 * [Builder] 또는 [ahoCorasick] DSL로 생성합니다. [Builder.build]가 반환된 뒤 automaton은 불변이며
 * 동시 검색에 안전합니다.
 *
 * **참고**: 내부 `TrieCore` 상태(failure transition)에 순환 참조가 있으므로
 * `AhoCorasickAutomaton<V>`는 `Serializable`을 구현하지 않습니다.
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
 * @param V 각 키워드에 연결된 값의 타입입니다.
 */
// 기존 facade를 유지하면서 eager 정렬과 scanner의 raw 순회를 별도 내부 경로로 구분합니다.
@Suppress("TooManyFunctions")
class AhoCorasickAutomaton<V> internal constructor(
    private val core: TrieCore,
    private val values: Map<String, V>,
    internal val options: SearchOptions,
) {

    internal val maxKeywordLength: Int = values.keys.maxOfOrNull { it.length } ?: 0

    companion object: KLogging() {
        /**
         * 새 [Builder] 인스턴스를 만듭니다.
         *
         * @param V 각 키워드에 연결된 값의 타입입니다.
         * @return 키워드와 옵션을 등록할 [Builder]입니다.
         */
        @JvmStatic
        fun <V> builder(): Builder<V> = Builder()
    }

    /**
     * [text]에서 등록된 모든 키워드를 검색하고 match 결과를 반환합니다.
     *
     * [SearchOptions.ignoreCase]가 `true`이면 검색 전에 [text]를 소문자로 바꿉니다.
     * [SearchOptions.stopOnFirstMatch]가 `true`이면 이 메서드는 첫 raw match 뒤에 탐색을 중단합니다.
     * [firstMatch]는 leftmost-longest 계약을 위해 이 설정을 무시하고 모든 후보를 확인합니다.
     * 결과의 [AhoCorasickMatch.keyword]는 정규화된 형태와, 필요하면 소문자 형태를 반영합니다.
     * 결과는 [AhoCorasickMatch.start] 오름차순, 동일한 시작 위치에서는 match 길이 내림차순,
     * 길이까지 같으면 keyword 오름차순으로 정렬됩니다. 이 eager 정렬은
     * [io.bluetape4k.text.search.flow.matchesAsFlow]의 기본
     * raw streaming 순서와 [AhoCorasickScanner]의 chunk 방출 순서에는 적용되지 않습니다.
     *
     * @param text 검색할 입력 문자열입니다.
     * @return start ASC, length DESC, keyword ASC 순서로 정렬된 match list입니다.
     */
    fun parseText(text: CharSequence): List<AhoCorasickMatch<V>> {
        if (text.isEmpty() || values.isEmpty()) {
            return emptyList()
        }

        val processed = preprocess(text)

        val emits = core.parseText(processed.text)
        return mapEmits(processed, emits, options.stopOnFirstMatch)
    }

    /**
     * 전체 결과 후처리가 필요한 Flow 경로를 문자 단위 취소 확인과 함께 수행합니다.
     *
     * [ignoreStopOnFirstMatch]가 `true`이면 Flow 계약에 따라 [SearchOptions.stopOnFirstMatch]를 무시합니다.
     * 겹침 제거와 단어 경계 필터는 raw match를 모두 수집한 뒤 적용하지만, trie 순회 자체는
     * [TrieCore.parseTextSuspending]을 통해 각 문자에서 협력 취소를 관찰합니다.
     * 후처리 결과는 eager [parseText]와 같은 match 정렬을 사용합니다.
     */
    internal suspend fun parseTextSuspending(
        text: CharSequence,
        ignoreStopOnFirstMatch: Boolean,
    ): List<AhoCorasickMatch<V>> {
        if (text.isEmpty() || values.isEmpty()) {
            return emptyList()
        }

        val processed = preprocess(text)
        val emits = core.parseTextSuspending(
            processed.text,
            stopOnHit = !ignoreStopOnFirstMatch && options.stopOnFirstMatch,
        )
        return mapEmits(processed, emits, !ignoreStopOnFirstMatch && options.stopOnFirstMatch)
    }

    /**
     * Scanner가 청크별 trie 순회 순서를 유지하도록 [parseText]와 같은 후처리를 적용합니다.
     *
     * [parseText]는 완전한 입력을 materialize한 뒤 start ASC 순서로 정렬하지만, scanner는 확정된
     * 청크를 즉시 방출해야 하므로 raw traversal 순서를 보존합니다.
     */
    internal fun parseTextStreaming(text: CharSequence): List<AhoCorasickMatch<V>> {
        if (text.isEmpty() || values.isEmpty()) {
            return emptyList()
        }

        val processed = preprocess(text)
        val emits = core.parseText(processed.text)
        return mapEmits(processed, emits, options.stopOnFirstMatch, sortMatches = false)
    }

    private fun mapEmits(
        processed: ProcessedText,
        emits: List<Emit>,
        stopOnFirstMatch: Boolean,
        sortMatches: Boolean = true,
    ): List<AhoCorasickMatch<V>> {
        if (emits.isEmpty()) {
            return emptyList()
        }

        val matches = ArrayList<AhoCorasickMatch<V>>(emits.size)
        for (emit in emits) {
            val keyword = emit.keyword ?: continue
            val value = values[keyword] ?: continue
            // 전처리된 offset을 원본 offset으로 복원합니다.
            val origStart = processed.toOriginalStart(emit.start)
            val origEnd = processed.toOriginalEndInclusive(emit.end)
            matches.add(
                AhoCorasickMatch(
                    start = origStart,
                    end = origEnd,
                    keyword = keyword,
                    value = value,
                )
            )
            if (stopOnFirstMatch) {
                break
            }
        }
        return if (!sortMatches || matches.size < 2) {
            matches
        } else {
            matches.sortedWith(
                compareBy<AhoCorasickMatch<V>> { it.start }
                    .thenByDescending { it.length }
                    .thenBy { it.keyword },
            )
        }
    }

    /** 청크 입력을 순차적으로 처리하는 상태 보존 scanner를 생성합니다. */
    fun scanner(): AhoCorasickScanner<V> = AhoCorasickScanner(this)

    /** 키워드가 등록되지 않은 빈 automaton인지 확인합니다. */
    internal fun isEmpty(): Boolean = values.isEmpty()

    /**
     * Trie를 순회하면서 원시 match를 찾을 때마다 [onMatch]를 호출합니다.
     *
     * 이 경로는 겹침 제거와 단어 경계 필터링 같은 후처리 필터를 의도적으로 우회합니다. 해당 필터가 필요한
     * 호출자는 [parseText]를 사용해야 합니다.
     *
     * @param text 검색할 입력 문자열입니다.
     * @param ignoreStopOnFirstMatch `true`이면 [SearchOptions.stopOnFirstMatch] 설정을 무시하고 원시 match를 계속 방출합니다.
     * @param onMatch 원시 match마다 호출할 suspend callback입니다.
     */
    internal suspend fun forEachRawMatch(
        text: CharSequence,
        ignoreStopOnFirstMatch: Boolean,
        onMatch: suspend (AhoCorasickMatch<V>) -> Unit,
    ) {
        if (text.isEmpty() || values.isEmpty()) {
            return
        }

        val processed = preprocess(text)

        core.runParseTextSuspending(
            processed.text,
            { emit ->
                val keyword = emit.keyword ?: return@runParseTextSuspending true
                val value = values[keyword] ?: return@runParseTextSuspending true
                val origStart = processed.toOriginalStart(emit.start)
                val origEnd = processed.toOriginalEndInclusive(emit.end)
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
     * [text]에서 leftmost-longest match를 반환합니다. Match가 없으면 `null`을 반환합니다.
     *
     * Leftmost-longest는 start가 가장 이른 match를 고르고, start가 같으면 가장 긴 match를 고르는 규칙입니다.
     * 이 계약을 보장하기 위해 [SearchOptions.stopOnFirstMatch] 설정과 관계없이 모든 후보를 확인합니다.
     *
     * @param text 검색할 입력 문자열입니다.
     * @return leftmost-longest [AhoCorasickMatch]입니다. Match가 없으면 `null`입니다.
     */
    fun firstMatch(text: CharSequence): AhoCorasickMatch<V>? {
        if (text.isEmpty() || values.isEmpty()) {
            return null
        }

        // firstMatch는 leftmost-longest 계약을 위해 stopOnFirstMatch와 무관하게 모든 후보를 확인합니다.
        val processed = preprocess(text)
        val emits = core.parseText(processed.text, stopOnHit = false)
        val matches = mapEmits(processed, emits, stopOnFirstMatch = false, sortMatches = false)
        // 가장 왼쪽(start ASC)을 우선하고, start가 같으면 더 긴 match(length DESC)를 우선합니다.
        return matches.minWithOrNull(
            compareBy<AhoCorasickMatch<V>> { it.start }.thenByDescending { it.length }
        )
    }

    /**
     * [text]에 등록된 키워드 match가 하나 이상 있으면 `true`를 반환합니다.
     *
     * 존재 여부만 확인할 때는 [parseText]보다 적은 작업으로 끝납니다. 단어 경계에 맞는 첫 match에서 중단합니다.
     *
     * @param text 검색할 입력 문자열입니다.
     * @return match가 하나 이상 있으면 `true`, 없으면 `false`입니다.
     */
    fun containsMatch(text: CharSequence): Boolean {
        if (text.isEmpty() || values.isEmpty()) return false
        return core.containsMatch(preprocess(text).text)
    }

    private data class ProcessedText(
        val text: String,
        val normalizationMapping: OffsetMapping?,
        val caseMapping: CaseFoldedText?,
    ) {
        fun toOriginalStart(offset: Int): Int {
            val normalizedOffset = caseMapping?.toSourceStart(offset) ?: offset
            return normalizationMapping?.toOriginal(normalizedOffset) ?: normalizedOffset
        }

        fun toOriginalEndInclusive(offset: Int): Int {
            val normalizedOffset = caseMapping?.toSourceEndInclusive(offset) ?: offset
            return normalizationMapping?.toOriginalEndInclusive(normalizedOffset) ?: normalizedOffset
        }
    }

    private fun preprocess(text: CharSequence): ProcessedText {
        val (normalizedText, normalizationMapping) = OffsetMapping.build(text, options.normalization)
        val caseMapping = if (options.ignoreCase) normalizedText.lowercaseWithMapping() else null
        return ProcessedText(
            text = caseMapping?.text ?: normalizedText,
            normalizationMapping = normalizationMapping,
            caseMapping = caseMapping,
        )
    }

    /**
     * [text]를 [SearchToken.Match]와 [SearchToken.Fragment] token sequence로 나눕니다.
     *
     * Match 사이의 비매치 구간과 마지막 match 뒤의 꼬리 문자열은 [SearchToken.Fragment]로 방출합니다.
     * 빈 입력은 빈 list를 반환합니다.
     *
     * @param text token으로 나눌 입력 문자열입니다.
     * @return text 순서를 유지하는 match/fragment token list입니다.
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

        // allowOverlaps=true일 때도 tokenize는 비겹침 시퀀스를 만들어야 하므로
        // replaceAll과 동일하게 start ASC, length DESC 정렬 후 겹침을 건너뜁니다.
        val sorted = matches.sortedWith(
            compareBy<AhoCorasickMatch<V>> { it.start }.thenByDescending { it.length }
        )

        for (match in sorted) {
            if (match.start < lastEnd) continue  // 앞 매치와 겹치면 건너뜁니다.
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
     * [text]에서 match된 모든 키워드를 [transform] 결과로 치환하고 새 문자열을 반환합니다.
     *
     * Match는 start-ASC / length-DESC 순서로 처리합니다. [SearchOptions.allowOverlaps]가 `true`여도
     * 이전 치환 cursor 기준으로 겹치는 match는 건너뜁니다. Match가 없으면 [text].toString()을
     * 그대로 반환합니다.
     *
     * @param text 처리할 입력 문자열입니다.
     * @param transform match를 치환 문자열로 매핑하는 함수입니다.
     * @return 치환 결과 문자열입니다.
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
        var cursor = 0  // exclusive cursor입니다.

        for (match in sorted) {
            // allowOverlaps=true인 경우에도 치환은 비겹침이어야 하므로 이전 매치와 겹치면 건너뜁니다.
            if (match.start < cursor) {
                continue
            }
            // 비매치 접두 구간입니다.
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
     * [AhoCorasickAutomaton]을 단계적으로 구성하는 builder입니다.
     *
     * ```kotlin
     * val automaton = AhoCorasickAutomaton.builder<Int>()
     *     .add("foo", 1)
     *     .add("bar", 2)
     *     .options(SearchOptions(ignoreCase = true, allowOverlaps = false))
     *     .build()
     * ```
     *
     * @param V 각 키워드에 연결된 값의 타입입니다.
     */
    class Builder<V> {
        companion object: KLogging()

        private val entries: MutableMap<String, V> = mutableMapOf()
        private var opts: SearchOptions = SearchOptions()

        /**
         * 키워드와 연결 값을 등록합니다.
         *
         * @param keyword 등록할 키워드입니다. Blank이면 안 됩니다.
         * @param value 키워드에 연결할 값입니다.
         * @return chaining을 위한 현재 builder입니다.
         */
        fun add(keyword: String, value: V): Builder<V> = apply {
            keyword.requireNotBlank("keyword")
            entries[keyword] = value
        }

        /**
         * Map에서 여러 키워드와 값 pair를 등록합니다.
         *
         * @param map 키워드를 값에 매핑한 map입니다. 각 key는 blank이면 안 됩니다.
         * @return chaining을 위한 현재 builder입니다.
         */
        fun addAll(map: Map<String, V>): Builder<V> = apply {
            map.forEach { (keyword, value) -> add(keyword, value) }
        }

        /**
         * 검색 옵션을 설정합니다.
         *
         * @param options 적용할 검색 옵션입니다.
         * @return chaining을 위한 현재 builder입니다.
         */
        fun options(options: SearchOptions): Builder<V> = apply {
            this.opts = options
        }

        /**
         * 등록한 키워드와 옵션으로 불변 [AhoCorasickAutomaton]을 생성합니다.
         *
         * [SearchOptions.ignoreCase]가 `true`이면 모든 키워드를 소문자로 바꾼 뒤 trie에 추가합니다.
         * 이렇게 해야 검색 시점 정규화와 생성 시점 정규화가 일치합니다.
         * 서로 다른 원본 키워드가 같은 정규화 key가 되면 value 소실을 막기 위해 예외를 던집니다.
         *
         * @return 생성이 끝난 불변 [AhoCorasickAutomaton]입니다.
         */
        fun build(): AhoCorasickAutomaton<V> {
            // 검색 시점과 동일한 파이프라인(NFC/NFKC 정규화 + ignoreCase)을 키워드에도 적용해야
            // 매치 일관성이 보장된다.
            val normalizedValues = HashMap<String, V>(entries.size)
            val normalizedKeywords = HashMap<String, String>(entries.size)
            entries.forEach { (keyword, value) ->
                val normalized = applyPipeline(keyword, opts)
                val previousKeyword = normalizedKeywords[normalized]
                if (previousKeyword != null) {
                    throw IllegalArgumentException(
                        "Normalization collision: keywords '$previousKeyword' and '$keyword' " +
                            "both normalize to '$normalized'"
                    )
                }
                normalizedKeywords[normalized] = keyword
                normalizedValues[normalized] = value
            }

            val coreBuilder = TrieCore.builder()
            normalizedValues.keys.forEach { coreBuilder.addKeyword(it) }

            // SearchOptions를 TrieCore Builder 옵션으로 매핑합니다.
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
