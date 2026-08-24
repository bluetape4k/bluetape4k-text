package io.bluetape4k.text.search.internal

import io.bluetape4k.text.search.internal.interval.IntervalTree
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.trace
import java.util.*

/**
 * Aho-Corasick trie의 내부 검색 엔진입니다.
 *
 * Aho-Corasick 논문([Bell technologies](http://cr.yp.to/bib/1975/aho.pdf))의 실패 전이 모델을
 * 사용합니다.
 *
 * ```
 * val trie = TrieCore.builder()
 *     .addKeywords("NYC")
 *     .addKeywords("APPL")
 *     .addKeywords("java_2e", "java programming")
 *     .addKeywords("PM", "product manager")
 *     .build()
 *
 * val text = "I am a PM for a java_2e platform working from APPL, NYC"
 * val emits = trie.parseText(text)
 *
 * emits shouldBeEqualTo listOf(
 *     Emit(7, 8, "PM"),
 *     Emit(16, 22, "java_2e"),
 *     Emit(46, 49, "APPL"),
 *     Emit(52, 54, "NYC")
 * )
 * ```
 *
 * 실패 전이는 [TrieCoreBuilder.build] 시점에 구성합니다. 대소문자 무시, 겹침 제거, 단어 경계 필터링 같은
 * 검색 동작은 [InternalTrieConfig]가 제어합니다.
 *
 * @property config trie 검색 설정입니다. 기본값은 [InternalTrieConfig.DEFAULT]입니다.
 */
internal class TrieCore(private val config: InternalTrieConfig = InternalTrieConfig.DEFAULT) {

    companion object: KLogging() {
        @JvmStatic
        /** [TrieCoreBuilder]를 생성합니다. */
        fun builder(): TrieCoreBuilder = TrieCoreBuilder()
    }

    private val rootState = State()

    private val ignoreCase: Boolean get() = config.ignoreCase

    /**
     * [text]에서 keyword match를 찾아 [map]의 치환 문자열로 바꾼 결과를 반환합니다.
     *
     * ```
     * val map = mapOf(
     *    "APPL" to "Apple",
     *    "NYC" to "New york",
     *    "java_2e" to "java programming",
     *    "PM" to "product manager"
     * )
     *
     * val replaced = trie.replace("I am a PM for a java_2e platform working from APPL, NYC", map)
     * replaced shouldBeEqualTo "I am a product manager for a java programming platform working from Apple, New york"
     * ```
     *
     * @param text 치환할 입력 문자열입니다.
     * @param map keyword를 치환 문자열에 매핑한 map입니다.
     * @return keyword match가 치환된 문자열입니다.
     *
     * [tokenize] 결과를 순회하면서 match token은 [map] 값으로 바꿉니다. [map]에 없는 keyword와 일반
     * fragment는 원문 그대로 둡니다.
     */
    fun replace(text: String, map: Map<String, String>): String {
        val tokens = tokenize(text)

        return buildString {
            tokens.forEach { token ->
                val keyword = token.emit?.keyword
                if (keyword != null && map.containsKey(keyword)) {
                    append(map[keyword])
                } else {
                    append(token.fragment)
                }
            }
        }
    }

    /**
     * [text]를 [MatchToken]과 [FragmentToken] 조각으로 나눕니다.
     *
     * ```
     * val GREEK_LETTERS = listOf("Alpha", "Beta", "Gamma")
     * val trie = TrieCore.builder()
     *     .addKeywords(GREEK_LETTERS)
     *     .build()
     *
     * val tokens = trie.tokenize("Alpha Beta Gamma").toList()
     * log.debug { "tokens=$tokens" }
     * tokens shouldHaveSize 5   // 2 space
     * ```
     *
     * @param text token으로 나눌 입력 문자열입니다.
     * @param destination 생성한 token을 누적할 mutable list입니다. 기본값은 새 list입니다.
     * @return 입력 순서를 유지하는 내부 token list입니다.
     */
    fun tokenize(
        text: String,
        destination: MutableList<InternalToken> = mutableListOf(),
    ): List<InternalToken> {
        if (text.isEmpty()) {
            return emptyList()
        }
        var lastCollectionIndex = -1
        val collectedEmits = parseText(text)

        collectedEmits
            .forEach { emit ->
                if (emit.start - lastCollectionIndex > 1) {
                    destination.add(createFragment(emit, text, lastCollectionIndex))
                }
                destination.add(createMatch(emit, text))
                lastCollectionIndex = emit.end
            }

        if (text.length - lastCollectionIndex > 1) {
            destination.add(createFragment(null, text, lastCollectionIndex))
        }
        return destination
    }

    /**
     * [text]를 파싱해 등록된 keyword match를 추출합니다.
     *
     * ```
     * val trie = TrieCore.builder()
     *     .addKeywords("NYC")
     *     .addKeywords("APPL")
     *     .addKeywords("java_2e", "java programming")
     *     .addKeywords("PM", "product manager")
     *     .build()
     *
     * val text = "I am a PM for a java_2e platform working from APPL, NYC"
     * val emits = trie.parseText(text)
     *
     * emits shouldBeEqualTo listOf(
     *     Emit(7, 8, "PM"),
     *     Emit(16, 22, "java_2e"),
     *     Emit(46, 49, "APPL"),
     *     Emit(52, 54, "NYC")
     * )
     * ```
     *
     * @param text 검색할 입력 문자열입니다.
     * @param emitHandler raw match를 수집할 emit handler입니다. 기본값은 [DefaultEmitHandler]입니다.
     * @return [InternalTrieConfig]의 필터를 적용한 [Emit] list입니다.
     *
     * 원시 parsing 뒤 [InternalTrieConfig]에 따라 부분 단어 match와 겹치는 match를 제거합니다.
     */
    fun parseText(text: CharSequence, emitHandler: StatefulEmitHandler = DefaultEmitHandler()): List<Emit> {
        runParseText(text, emitHandler)
        var collectedEmits = emitHandler.emits

        if (config.onlyWholeWords) {
            removePartialMatches(text, collectedEmits)
            log.trace { "onlyWholeWords : collectedEmits=$collectedEmits" }
        }
        if (config.onlyWholeWordsWhiteSpaceSeparated) {
            removePartialMatchesWhiteSpaceSeparated(text, collectedEmits)
            log.trace { "onlyWholeWordsWhiteSpaceSeparated : collectedEmits=$collectedEmits" }
        }
        if (!config.allowOverlaps) {
            val intervalTree = IntervalTree(collectedEmits)
            collectedEmits = intervalTree.removeOverlaps(collectedEmits)
            log.trace { "!allowOverlaps : collectedEmits=$collectedEmits" }
        }

        return collectedEmits
    }

    /** [text]에 등록된 keyword match가 하나 이상 있으면 `true`를 반환합니다. */
    fun containsMatch(text: CharSequence): Boolean = firstMatch(text) != null

    /**
     * [text]를 문자 단위로 순회하고 keyword match를 찾을 때마다 [emitHandler]를 호출합니다.
     *
     * ```
     *  val PRONOUNS = listOf("hers", "his", "she", "he")
     *  val trie = TrieCore.builder()
     *      .addKeywords(PRONOUNS)
     *      .build()
     *
     *  val emits = mutableListOf<Emit>()
     *  val emitHandler = EmitHandler { emit -> emits }
     *
     *  trie.runParseText("ushers", emitHandler)
     *
     *  emits shouldHaveSize 3
     *  checkEmit(emits[0], 2, 3, "he")
     *  checkEmit(emits[1], 1, 3, "she")
     *  checkEmit(emits[2], 2, 5, "hers")
     *  ```
     *
     *  @param text 검색할 입력 문자열입니다.
     *  @param emitHandler keyword match마다 호출할 emit handler입니다.
     *  @param stopOnHit `true`이면 [emitHandler]가 `true`를 반환한 첫 match에서 검색을 중단합니다.
     *
     *  상태 전이를 문자 단위로 수행하고 각 emit을 [emitHandler]에 전달합니다. [config.stopOnHit]이
     *  `true`이고 handler가 `true`를 반환하면 즉시 중단합니다.
     */
    fun runParseText(
        text: CharSequence,
        emitHandler: EmitHandler,
        stopOnHit: Boolean = config.stopOnHit,
    ) {
        var currentState = rootState

        text.forEachIndexed { pos, ch ->
            currentState = when {
                config.ignoreCase -> getState(currentState, ch.lowercaseChar())
                else -> getState(currentState, ch)
            }
            val stored = storeEmits(pos, currentState, emitHandler, stopOnHit)
            if (stored && stopOnHit) {
                return
            }
        }
    }

    /**
     * [text]를 문자 단위로 순회하고 keyword match를 찾을 때마다 suspending [emitHandler]를 호출합니다.
     *
     * @param text 검색할 입력 문자열입니다.
     * @param emitHandler keyword match마다 호출할 suspending handler입니다.
     * @param stopOnHit `true`이면 [emitHandler]가 `true`를 반환한 첫 match에서 검색을 중단합니다.
     */
    suspend fun runParseTextSuspending(
        text: CharSequence,
        emitHandler: suspend (Emit) -> Boolean,
        stopOnHit: Boolean = config.stopOnHit,
    ) {
        var currentState = rootState

        text.forEachIndexed { pos, ch ->
            currentState = when {
                config.ignoreCase -> getState(currentState, ch.lowercaseChar())
                else -> getState(currentState, ch)
            }
            val stored = storeEmitsSuspending(pos, currentState, emitHandler, stopOnHit)
            if (stored && stopOnHit) {
                return
            }
        }
    }

    /**
     * [text]에서 첫 keyword match를 반환합니다. Match가 없으면 `null`을 반환합니다.
     *
     * ```
     * val UNICODE = listOf("turning", "once", "again", "börkü")
     *
     * val trie = TrieCore.builder()
     *     .ignoreCase()
     *     .onlyWholeWords()
     *     .addKeywords(UNICODE)
     *     .build()
     *
     * val firstMatch = trie.firstMatch("TurninG OnCe AgAiN BÖRKÜ")
     * checkEmit(firstMatch, 0, 6, "turning")
     * ```
     *
     * @param text 검색할 입력 문자열입니다.
     * @return 첫 [Emit] match입니다. Match가 없으면 `null`입니다.
     *
     * [InternalTrieConfig.allowOverlaps]가 `false`이면 [parseText]에 위임한 뒤 첫 결과를 반환합니다.
     * [InternalTrieConfig.onlyWholeWords] 필터도 동일하게 적용합니다.
     */
    fun firstMatch(text: CharSequence): Emit? {
        if (!config.allowOverlaps) {
            return parseText(text).firstOrNull()
        }

        var currentState = rootState

        text.forEachIndexed { pos, ch ->
            currentState = when {
                config.ignoreCase -> getState(currentState, ch.lowercaseChar())
                else -> getState(currentState, ch)
            }

            currentState.emit().forEach { emitStr ->
                val emit = Emit(pos - emitStr.length + 1, pos, emitStr)
                if (config.onlyWholeWords) {
                    if (!isPartialMatch(text, emit)) {
                        return emit
                    }
                } else {
                    return emit
                }
            }
        }
        log.trace { "Not found matches. text=$text" }
        return null
    }

    private fun addKeyword(keyword: String) {
        if (keyword.isNotEmpty()) {
            val adder = if (ignoreCase) keyword.lowercaseCharByChar() else keyword
            addState(adder).addEmit(adder)
        }
    }

    private fun addKeywords(vararg keywords: String) {
        keywords.forEach { addKeyword(it) }
    }

    private fun addKeywords(keywords: Collection<String>) {
        keywords.forEach { addKeyword(it) }
    }

    private fun addState(keyword: String): State = rootState.addState(keyword)

    private fun createFragment(emit: Emit?, text: String, lastcollectedPosition: Int): InternalToken {
        return FragmentToken(text.substring(lastcollectedPosition + 1, emit?.start ?: text.length))
    }

    private fun createMatch(emit: Emit, text: String): InternalToken {
        return MatchToken(text.substring(emit.start, emit.end + 1), emit)
    }

    private fun isPartialMatch(searchText: CharSequence, emit: Emit): Boolean {
        val isAlphabeticStart =
            emit.start != 0 && Character.isAlphabetic(searchText[emit.start - 1].code)

        if (isAlphabeticStart) {
            return true
        }

        val isAlphabeticEnd: Boolean =
            emit.end + 1 != searchText.length && Character.isAlphabetic(searchText[emit.end + 1].code)

        return isAlphabeticEnd
    }

    private fun removePartialMatches(searchText: CharSequence, collectedEmits: MutableList<Emit>) {
        collectedEmits.removeIf { isPartialMatch(searchText, it) }
    }

    private fun removePartialMatchesWhiteSpaceSeparated(searchText: CharSequence, collectedEmits: MutableList<Emit>) {
        val size = searchText.length

        collectedEmits.removeIf { emit ->
            val isEmptyStart = emit.start == 0 || Character.isWhitespace(searchText[emit.start - 1])
            if (!isEmptyStart) {
                true
            } else {
                val isEmptyEnd = emit.end + 1 == size || Character.isWhitespace(searchText[emit.end + 1])
                !isEmptyEnd
            }
        }
    }

    private fun getState(currentState: State, ch: Char): State {
        var thisState = currentState
        var nextState = thisState.nextState(ch)
        while (nextState == null) {
            thisState = requireNotNull(thisState.failure) { "failure state must be set after constructFailureStates()" }
            nextState = thisState.nextState(ch)
        }
        return nextState
    }

    private fun constructFailureStates() {
        val queue = ArrayDeque<State>()
        val startState = rootState

        // 첫 단계: depth 1 상태의 failure를 root 상태로 연결합니다.
        startState.getStates().forEach { depthOneState ->
            depthOneState.failure = startState
            queue.add(depthOneState)
        }

        // 둘째 단계: depth > 1 상태의 failure를 계산합니다.
        while (queue.isNotEmpty()) {
            val currentState = queue.remove()
            log.trace { "currentState=$currentState" }

            currentState.getTransitions().forEach { transition ->
                val targetState = currentState.nextState(transition)
                check(targetState != null) {
                    "targetState must not be null. transition=$transition, currentState=$currentState"
                }
                queue.add(targetState)

                var traceFailureState = requireNotNull(currentState.failure) { "failure state must be set" }
                while (traceFailureState.nextState(transition) == null) {
                    traceFailureState = requireNotNull(traceFailureState.failure) { "failure state must be set" }
                }

                val newFailureState = requireNotNull(traceFailureState.nextState(transition)) { "nextState must exist after failure traversal" }
                targetState.failure = newFailureState
                targetState.addEmits(newFailureState.emit())
            }
        }
    }

    private fun storeEmits(
        position: Int,
        currentState: State,
        emitHandler: EmitHandler,
        stopOnHit: Boolean,
    ): Boolean {
        var emitted = false
        val emits = currentState.emit()

        emits.forEach { emit ->
            emitted = emitHandler.emit(Emit(position - emit.length + 1, position, emit))
            if (emitted && stopOnHit) {
                return true
            }
        }
        return emitted
    }

    private suspend fun storeEmitsSuspending(
        position: Int,
        currentState: State,
        emitHandler: suspend (Emit) -> Boolean,
        stopOnHit: Boolean,
    ): Boolean {
        var emitted = false
        val emits = currentState.emit()

        emits.forEach { emit ->
            emitted = emitHandler(Emit(position - emit.length + 1, position, emit))
            if (emitted && stopOnHit) {
                return true
            }
        }
        return emitted
    }


    class TrieCoreBuilder {
        private val configBuilder = InternalTrieConfig.builder()
        private val keywords: MutableList<String> = mutableListOf()

        /** 단일 keyword를 추가합니다. */
        fun addKeyword(keyword: String) = apply {
            this.keywords.add(keyword)
        }

        /** 여러 keyword를 추가합니다. */
        fun addKeywords(vararg keywords: String) = apply {
            this.keywords.addAll(keywords)
        }

        /** Collection에 담긴 keyword를 추가합니다. */
        fun addKeywords(keywords: Collection<String>) = apply {
            this.keywords.addAll(keywords)
        }

        /** 겹치는 match를 허용하지 않도록 설정합니다. */
        fun ignoreOverlaps() = apply {
            configBuilder.allowOverlaps(false)
        }

        /** 알파벳 경계를 기준으로 whole-word match만 허용합니다. */
        fun onlyWholeWords() = apply {
            configBuilder.onlyWholeWords(true)
        }

        /** 공백 경계를 기준으로 whole-word match만 허용합니다. */
        fun onlyWholeWordsWhiteSpaceSeparated() = apply {
            configBuilder.onlyWholeWordsWhiteSpaceSeparated(true)
        }

        /** 대소문자를 구분하지 않는 match를 활성화합니다. */
        fun ignoreCase() = apply {
            configBuilder.ignoreCase(true)
        }

        /** 첫 match에서 중단하는 mode를 활성화합니다. */
        fun stopOnHit() = apply {
            configBuilder.stopOnHit(true)
        }

        /**
         * 실패 전이가 구성되어 바로 검색할 수 있는 [TrieCore]를 생성합니다.
         *
         * @return 등록한 keyword와 설정을 반영한 [TrieCore]입니다.
         */
        fun build(): TrieCore {
            return TrieCore(configBuilder.build()).apply {
                addKeywords(keywords)
                constructFailureStates()
            }
        }
    }
}
