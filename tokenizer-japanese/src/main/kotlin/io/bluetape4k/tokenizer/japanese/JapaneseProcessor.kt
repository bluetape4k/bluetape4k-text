package io.bluetape4k.tokenizer.japanese

import com.atilika.kuromoji.ipadic.Token
import io.bluetape4k.logging.KLogging
import io.bluetape4k.tokenizer.japanese.block.JapaneseBlockwordProcessor
import io.bluetape4k.tokenizer.japanese.tokenizer.JapaneseTokenizer
import io.bluetape4k.tokenizer.japanese.utils.JapaneseDictionaryProvider
import io.bluetape4k.tokenizer.model.BlockwordRequest
import io.bluetape4k.tokenizer.model.BlockwordResponse
import io.bluetape4k.tokenizer.model.requireTokenizeTextLength

/**
 * Facade for Japanese morphological tokenization and blockword detection/masking.
 *
 * Delegates tokenization to [JapaneseTokenizer] and blockword operations to
 * [JapaneseBlockwordProcessor] and [JapaneseDictionaryProvider].
 *
 * ```kotlin
 * val nouns = JapaneseProcessor
 *     .filterNoun(JapaneseProcessor.tokenize("私は、日本語の勉強をしています。"))
 *     .map { it.surface }
 *
 * // result == ["私", "日本語", "勉強"]
 * ```
 */
object JapaneseProcessor: KLogging() {

    /**
     * Tokenizes the input sentence into a list of morphological tokens.
     *
     * Rejects inputs longer than `MAX_TOKENIZE_TEXT_LENGTH` before invoking
     * Kuromoji.
     *
     * ```kotlin
     * val tokens = JapaneseProcessor.tokenize("お寿司が食べたい。")
     * val surfaces = tokens.map { it.surface }
     *
     * // result == ["お", "寿司", "が", "食べ", "たい", "。"]
     * ```
     */
    fun tokenize(text: String): List<Token> {
        requireTokenizeTextLength(text)
        return JapaneseTokenizer.tokenize(text)
    }

    /**
     * Filters the token list to those matching the given predicate.
     *
     * ```kotlin
     * val tokens = JapaneseProcessor.tokenize("お寿司が食べたい。")
     * val nouns = JapaneseProcessor.filter(tokens) { it.isNoun() }.map { it.surface }
     *
     * // result == ["寿司"]
     * ```
     */
    fun filter(tokens: List<Token>, predicate: (Token) -> Boolean): List<Token> {
        return JapaneseTokenizer.filter(tokens, predicate)
    }

    /**
     * Filters the token list to nouns (`名詞`) only.
     *
     * ```kotlin
     * val tokens = JapaneseProcessor.tokenize("私は、日本語の勉強をしています。")
     * val nouns = JapaneseProcessor.filterNoun(tokens).map { it.surface }
     *
     * // result == ["私", "日本語", "勉強"]
     * ```
     */
    fun filterNoun(tokens: List<Token>): List<Token> {
        return JapaneseTokenizer.filterNoun(tokens)
    }

    /**
     * Finds tokens in the sentence that match entries in the blockword dictionary.
     *
     * Falls back to compound-word matching (noun+noun/verb combinations) when
     * direct token matching fails.
     *
     * ```kotlin
     * val blockwords = JapaneseProcessor.findBlockwords("ホモの男性を理解できない").map { it.surface }
     *
     * // result == ["ホモ"]
     * ```
     */
    fun findBlockwords(text: String): List<Token> {
        return JapaneseBlockwordProcessor.findBlockwords(text)
    }

    /**
     * Replaces blockword tokens in the sentence with a mask string.
     *
     * ```kotlin
     * val request = io.bluetape4k.tokenizer.model.blockwordRequestOf("ホモの男性を理解できない")
     * val response = JapaneseProcessor.maskBlockwords(request)
     *
     * // response.maskedText == "**の男性を理解できない"
     * ```
     */
    fun maskBlockwords(request: BlockwordRequest): BlockwordResponse {
        return JapaneseBlockwordProcessor.maskBlockwords(request)
    }

    /**
     * Adds words to the in-memory blockword dictionary.
     *
     * ```kotlin
     * JapaneseProcessor.addBlockwords(listOf("東京"))
     * val found = JapaneseProcessor.findBlockwords("これは東京です").map { it.surface }
     *
     * // result == ["東京"]
     * ```
     */
    fun addBlockwords(words: List<String>) {
        JapaneseDictionaryProvider.addBlockwords(words)
    }

    /**
     * Removes words from the in-memory blockword dictionary. Unknown words are silently ignored.
     *
     * ```kotlin
     * JapaneseProcessor.addBlockwords(listOf("東京"))
     * JapaneseProcessor.removeBlockwords(listOf("東京"))
     * val found = JapaneseProcessor.findBlockwords("これは東京です")
     *
     * // result == []
     * ```
     */
    fun removeBlockwords(words: List<String>) {
        JapaneseDictionaryProvider.removeBlockwords(words)
    }

    /**
     * Clears the in-memory blockword dictionary. Does not delete the underlying resource files.
     *
     * ```kotlin
     * JapaneseProcessor.clearBlockwords()
     * val found = JapaneseProcessor.findBlockwords("ホモの男性を理解できない")
     *
     * // result == []
     * ```
     */
    fun clearBlockwords() {
        JapaneseDictionaryProvider.clearBlockwords()
    }
}
