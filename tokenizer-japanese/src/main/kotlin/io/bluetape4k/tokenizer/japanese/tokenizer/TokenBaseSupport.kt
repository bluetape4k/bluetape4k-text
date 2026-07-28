package io.bluetape4k.tokenizer.japanese.tokenizer

import com.atilika.kuromoji.TokenBase

/**
 * 토큰의 기본 품사(`allFeaturesArray[0]`)가 명사(`名詞`)인지 확인합니다.
 *
 * ```kotlin
 * val tokens = JapaneseTokenizer.tokenize("お寿司が食べたい。")
 * val value = tokens.first { it.surface == "寿司" }.isNoun()
 *
 * // value == true
 * ```
 *
 * @return 기본 품사가 명사이면 `true`입니다.
 */
fun TokenBase.isNoun(): Boolean = this.allFeaturesArray[0] == "名詞"

/**
 * 토큰의 기본 품사(`allFeaturesArray[0]`)가 동사(`動詞`)인지 확인합니다.
 *
 * ```kotlin
 * val tokens = JapaneseTokenizer.tokenize("お寿司が食べたい。")
 * val value = tokens.first { it.surface == "食べ" }.isVerb()
 *
 * // value == true
 * ```
 *
 * @return 기본 품사가 동사이면 `true`입니다.
 */
fun TokenBase.isVerb(): Boolean = this.allFeaturesArray[0] == "動詞"

/**
 * 토큰의 기본 품사가 명사(`名詞`) 또는 동사(`動詞`)인지 확인합니다.
 *
 * ```kotlin
 * val tokens = JapaneseTokenizer.tokenize("お寿司が食べたい。")
 * val value = tokens.first { it.surface == "が" }.isNounOrVerb()
 *
 * // value == false
 * ```
 *
 * @return 기본 품사가 명사나 동사이면 `true`입니다.
 */
fun TokenBase.isNounOrVerb(): Boolean = this.isNoun() || this.isVerb()

/**
 * 토큰의 기본 품사가 형용사(`形容詞`)인지 확인합니다.
 *
 * ```kotlin
 * val tokens = JapaneseTokenizer.tokenize("美しい花")
 * val value = tokens.first { it.surface == "美しい" }.isAdjective()
 *
 * // value == true
 * ```
 *
 * @return 기본 품사가 형용사이면 `true`입니다.
 */
fun TokenBase.isAdjective(): Boolean = this.allFeaturesArray[0] == "形容詞"

/**
 * 토큰의 기본 품사가 조사(`助詞`)인지 확인합니다.
 *
 * ```kotlin
 * val tokens = JapaneseTokenizer.tokenize("お寿司が食べたい。")
 * val value = tokens.first { it.surface == "が" }.isJosa()
 *
 * // value == true
 * ```
 *
 * @return 기본 품사가 조사이면 `true`입니다.
 */
fun TokenBase.isJosa(): Boolean = this.allFeaturesArray[0] == "助詞"

/**
 * 토큰의 기본 품사가 조동사(`助動詞`)인지 확인합니다.
 *
 * ```kotlin
 * val tokens = JapaneseTokenizer.tokenize("お寿司が食べたい。")
 * val value = tokens.first { it.surface == "たい" }.isConjugate()
 *
 * // value == true
 * ```
 *
 * @return 기본 품사가 조동사이면 `true`입니다.
 */
fun TokenBase.isConjugate(): Boolean = this.allFeaturesArray[0] == "助動詞"

/**
 * 토큰의 기본 품사가 문장부호를 포함한 기호(`記号`)인지 확인합니다.
 *
 * ```kotlin
 * val tokens = JapaneseTokenizer.tokenize("お寿司が食べたい。")
 * val value = tokens.first { it.surface == "。" }.isPunctuation()
 *
 * // value == true
 * ```
 *
 * @return 기본 품사가 기호이면 `true`입니다.
 */
fun TokenBase.isPunctuation(): Boolean = this.allFeaturesArray[0] == "記号"
