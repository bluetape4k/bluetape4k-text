package io.bluetape4k.text.search.internal

import io.bluetape4k.text.search.internal.interval.IntervalTree
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.trace
import java.util.*

/**
 * TrieCore
 *
 * Based on the Aho-Corasick white paper, [Bell technologies](http://cr.yp.to/bib/1975/aho.pdf)
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
 * Failure transitions are constructed at [build] time. Search behavior is controlled by
 * [InternalTrieConfig] (case-insensitive, overlap removal, word-boundary filtering).
 *
 * @property config trie configuration (default: [InternalTrieConfig.DEFAULT])
 */
internal class TrieCore(private val config: InternalTrieConfig = InternalTrieConfig.DEFAULT) {

    companion object: KLogging() {
        @JvmStatic
        /** Creates a [TrieCoreBuilder]. */
        fun builder(): TrieCoreBuilder = TrieCoreBuilder()
    }

    private val rootState = State()

    private val ignoreCase: Boolean get() = config.ignoreCase

    /**
     * Replaces all keyword matches in [text] with their mapped values from [map] and returns the resulting string.
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
     * @param text input text to process
     * @param map keyword-to-replacement map
     *
     * Iterates [tokenize] results and replaces matched tokens with their [map] values.
     * Matches with no map entry are kept as-is.
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
     * Splits [text] into [MatchToken] and [FragmentToken] segments.
     *
     * ```
     * val GREEK_LETTERS = listOf("Alpha", "Beta", "Gamma")
     * val trie = TrieCore.builder()
     *     .addKeywords(GREEK_LETTERS)
     *     .build()
     *
     * val tokens = trie.tokenize("Alpha Beta Gamma").toList()
     * log.debug { "tokens=$tokens" }
     * tokens.size shouldBeEqualTo 5   // 2 space
     * ```
     *
     * @param text input text to tokenize
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
     * 문장을 파싱하여 키워드를 추출합니다.
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
     * @param text input text to search
     * @param emitHandler emit handler (default: [DefaultEmitHandler])
     *
     * Applies partial-match and overlap filters per [InternalTrieConfig] after raw parsing.
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

    /** Returns `true` if [text] contains at least one matching keyword. */
    fun containsMatch(text: CharSequence): Boolean = firstMatch(text) != null

    /**
     * Parses [text] char-by-char, invoking [emitHandler] for each keyword match found.
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
     *  emits.size shouldBeEqualTo 3
     *  checkEmit(emits[0], 2, 3, "he")
     *  checkEmit(emits[1], 1, 3, "she")
     *  checkEmit(emits[2], 2, 5, "hers")
     *  ```
     *
     *  @param text input text to search
     *  @param emitHandler emit handler to receive each keyword match
     *
     *  Performs char-by-char state transitions and delivers each emit to [emitHandler].
     *  Stops immediately when [config.stopOnHit] is `true` and the handler returns `true`.
     */
    fun runParseText(text: CharSequence, emitHandler: EmitHandler) {
        var currentState = rootState

        text.forEachIndexed { pos, ch ->
            currentState = when {
                config.ignoreCase -> getState(currentState, ch.lowercaseChar())
                else -> getState(currentState, ch)
            }
            val stored = storeEmits(pos, currentState, emitHandler)
            if (stored && config.stopOnHit) {
                return
            }
        }
    }

    /**
     * Returns the first keyword match in [text], or `null` if none is found.
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
     * @param text input text to search
     * @return the first [Emit] match, or `null` if none
     *
     * When [allowOverlaps] is `false`, delegates to [parseText] and returns the first result.
     * Respects [onlyWholeWords] filtering.
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
            val adder = if (ignoreCase) keyword.lowercase() else keyword
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

        // First, set the fail state of all depth 1 states to the root state
        startState.getStates().forEach { depthOneState ->
            depthOneState.failure = startState
            queue.add(depthOneState)
        }

        // Second, determine the fail state for all depth > 1 state
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

    private fun storeEmits(position: Int, currentState: State, emitHandler: EmitHandler): Boolean {
        var emitted = false
        val emits = currentState.emit()

        emits.forEach { emit ->
            emitted = emitHandler.emit(Emit(position - emit.length + 1, position, emit))
            if (emitted && config.stopOnHit) {
                return true
            }
        }
        return emitted
    }


    class TrieCoreBuilder {
        private val configBuilder = InternalTrieConfig.builder()
        private val keywords: MutableList<String> = mutableListOf()

        /** Adds a single keyword. */
        fun addKeyword(keyword: String) = apply {
            this.keywords.add(keyword)
        }

        /** Adds multiple keywords. */
        fun addKeywords(vararg keywords: String) = apply {
            this.keywords.addAll(keywords)
        }

        /** Adds keywords from a collection. */
        fun addKeywords(keywords: Collection<String>) = apply {
            this.keywords.addAll(keywords)
        }

        /** Disallows overlapping matches. */
        fun ignoreOverlaps() = apply {
            configBuilder.allowOverlaps(false)
        }

        /** Restricts matching to whole words (alphabetic boundary). */
        fun onlyWholeWords() = apply {
            configBuilder.onlyWholeWords(true)
        }

        /** Restricts matching to whole words (whitespace boundary). */
        fun onlyWholeWordsWhiteSpaceSeparated() = apply {
            configBuilder.onlyWholeWordsWhiteSpaceSeparated(true)
        }

        /** Enables case-insensitive matching. */
        fun ignoreCase() = apply {
            configBuilder.ignoreCase(true)
        }

        /** Enables stop-on-first-hit mode. */
        fun stopOnHit() = apply {
            configBuilder.stopOnHit(true)
        }

        /**
         * Builds a [TrieCore] with failure transitions constructed and ready for search.
         */
        fun build(): TrieCore {
            return TrieCore(configBuilder.build()).apply {
                addKeywords(keywords)
                constructFailureStates()
            }
        }
    }
}
