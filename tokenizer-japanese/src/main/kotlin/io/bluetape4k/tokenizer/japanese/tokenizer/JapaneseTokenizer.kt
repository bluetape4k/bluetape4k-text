package io.bluetape4k.tokenizer.japanese.tokenizer

import com.atilika.kuromoji.ipadic.Token
import com.atilika.kuromoji.ipadic.Tokenizer
import io.bluetape4k.logging.KLogging

/**
 * Entry point for Japanese morphological analysis using the Kuromoji IPAdic tokenizer.
 *
 * The internal [com.atilika.kuromoji.ipadic.Tokenizer] instance is lazily initialized and
 * reused for the object's lifetime. Use [filter] or [filterNoun] for POS-based post-filtering.
 *
 * ```kotlin
 * val tokens = JapaneseTokenizer.tokenize("お寿司が食べたい。")
 * val surfaces = tokens.map { it.surface }
 *
 * // result == ["お", "寿司", "が", "食べ", "たい", "。"]
 * ```
 */
object JapaneseTokenizer: KLogging() {

    internal val tokenizer: Tokenizer by lazy { Tokenizer.Builder().build() }

    /**
     * Analyzes the input sentence into a list of morphological tokens using Kuromoji IPAdic rules.
     *
     * ```kotlin
     * val tokens = JapaneseTokenizer.tokenize("お寿司が食べたい。")
     * val surfaces = tokens.map { it.surface }
     *
     * // result == ["お", "寿司", "が", "食べ", "たい", "。"]
     * ```
     */
    fun tokenize(text: String): List<Token> {
        return tokenizer.tokenize(text)
    }

    /**
     * Filters the token list to those satisfying the given predicate.
     *
     * ```kotlin
     * val tokens = JapaneseTokenizer.tokenize("お寿司が食べたい。")
     * val punctuations = JapaneseTokenizer.filter(tokens) { it.isPunctuation() }
     *
     * // result == ["。"]
     * ```
     */
    fun filter(tokens: List<Token>, predicate: (Token) -> Boolean): List<Token> {
        return tokens.filter(predicate)
    }

    /**
     * Filters the token list to nouns (`名詞`) only.
     *
     * ```kotlin
     * val tokens = JapaneseTokenizer.tokenize("私は、日本語の勉強をしています。")
     * val nouns = JapaneseTokenizer.filterNoun(tokens).map { it.surface }
     *
     * // result == ["私", "日本語", "勉強"]
     * ```
     */
    fun filterNoun(tokens: List<Token>): List<Token> {
        return filter(tokens) { it.isNoun() }
    }
}
