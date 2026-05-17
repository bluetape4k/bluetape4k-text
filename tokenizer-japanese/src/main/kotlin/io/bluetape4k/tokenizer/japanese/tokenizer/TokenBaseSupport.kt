package io.bluetape4k.tokenizer.japanese.tokenizer

import com.atilika.kuromoji.TokenBase

/**
 * Returns `true` if the token's primary POS (`allFeaturesArray[0]`) is a noun (`名詞`).
 *
 * ```kotlin
 * val tokens = JapaneseTokenizer.tokenize("お寿司が食べたい。")
 * val value = tokens.first { it.surface == "寿司" }.isNoun()
 *
 * // value == true
 * ```
 */
fun TokenBase.isNoun(): Boolean = this.allFeaturesArray[0] == "名詞"

/**
 * Returns `true` if the token's primary POS (`allFeaturesArray[0]`) is a verb (`動詞`).
 *
 * ```kotlin
 * val tokens = JapaneseTokenizer.tokenize("お寿司が食べたい。")
 * val value = tokens.first { it.surface == "食べ" }.isVerb()
 *
 * // value == true
 * ```
 */
fun TokenBase.isVerb(): Boolean = this.allFeaturesArray[0] == "動詞"

/**
 * Returns `true` if the token is a noun (`名詞`) or a verb (`動詞`).
 *
 * ```kotlin
 * val tokens = JapaneseTokenizer.tokenize("お寿司が食べたい。")
 * val value = tokens.first { it.surface == "が" }.isNounOrVerb()
 *
 * // value == false
 * ```
 */
fun TokenBase.isNounOrVerb(): Boolean = this.isNoun() || this.isVerb()

/**
 * Returns `true` if the token's primary POS is an adjective (`形容詞`).
 *
 * ```kotlin
 * val tokens = JapaneseTokenizer.tokenize("美しい花")
 * val value = tokens.first { it.surface == "美しい" }.isAdjective()
 *
 * // value == true
 * ```
 */
fun TokenBase.isAdjective(): Boolean = this.allFeaturesArray[0] == "形容詞"

/**
 * Returns `true` if the token's primary POS is a particle (`助詞`).
 *
 * ```kotlin
 * val tokens = JapaneseTokenizer.tokenize("お寿司が食べたい。")
 * val value = tokens.first { it.surface == "が" }.isJosa()
 *
 * // value == true
 * ```
 */
fun TokenBase.isJosa(): Boolean = this.allFeaturesArray[0] == "助詞"

/**
 * Returns `true` if the token's primary POS is an auxiliary verb (`助動詞`).
 *
 * ```kotlin
 * val tokens = JapaneseTokenizer.tokenize("お寿司が食べたい。")
 * val value = tokens.first { it.surface == "たい" }.isConjugate()
 *
 * // value == true
 * ```
 */
fun TokenBase.isConjugate(): Boolean = this.allFeaturesArray[0] == "助動詞"

/**
 * Returns `true` if the token's primary POS is a symbol (`記号`), including punctuation.
 *
 * ```kotlin
 * val tokens = JapaneseTokenizer.tokenize("お寿司が食べたい。")
 * val value = tokens.first { it.surface == "。" }.isPunctuation()
 *
 * // value == true
 * ```
 */
fun TokenBase.isPunctuation(): Boolean = this.allFeaturesArray[0] == "記号"
