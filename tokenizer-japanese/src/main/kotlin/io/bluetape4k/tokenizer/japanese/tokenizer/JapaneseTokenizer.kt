package io.bluetape4k.tokenizer.japanese.tokenizer

import com.atilika.kuromoji.ipadic.Token
import com.atilika.kuromoji.ipadic.Tokenizer
import io.bluetape4k.logging.KLogging
import io.bluetape4k.tokenizer.model.requireTokenizeTextLength

/**
 * Kuromoji IPADic 토크나이저를 사용하는 일본어 형태소 분석 진입점입니다.
 *
 * 내부 [com.atilika.kuromoji.ipadic.Tokenizer] 인스턴스는 lazy로 초기화하고 객체 수명 동안 재사용합니다.
 * 품사 기반 후처리는 [filter] 또는 [filterNoun]을 사용합니다.
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
     * 입력 문장을 Kuromoji IPADic 규칙으로 형태소 토큰 목록으로 분석합니다.
     *
     * ```kotlin
     * val tokens = JapaneseTokenizer.tokenize("お寿司が食べたい。")
     * val surfaces = tokens.map { it.surface }
     *
     * // result == ["お", "寿司", "が", "食べ", "たい", "。"]
     * ```
     *
     * @param text 형태소 분석할 일본어 입력 문장입니다.
     * @return Kuromoji IPADic 규칙으로 분석한 토큰 목록입니다.
     */
    fun tokenize(text: String): List<Token> {
        requireTokenizeTextLength(text)
        return tokenizer.tokenize(text)
    }

    /**
     * 토큰 목록에서 [predicate]를 만족하는 토큰만 반환합니다.
     *
     * ```kotlin
     * val tokens = JapaneseTokenizer.tokenize("お寿司が食べたい。")
     * val punctuations = JapaneseTokenizer.filter(tokens) { it.isPunctuation() }
     *
     * // result == ["。"]
     * ```
     *
     * @param tokens 필터링할 Kuromoji 토큰 목록입니다.
     * @param predicate 유지할 토큰을 판정하는 조건 함수입니다.
     * @return 조건을 만족하는 토큰 목록입니다.
     */
    fun filter(tokens: List<Token>, predicate: (Token) -> Boolean): List<Token> {
        return tokens.filter(predicate)
    }

    /**
     * 토큰 목록에서 명사(`名詞`) 토큰만 반환합니다.
     *
     * ```kotlin
     * val tokens = JapaneseTokenizer.tokenize("私は、日本語の勉強をしています。")
     * val nouns = JapaneseTokenizer.filterNoun(tokens).map { it.surface }
     *
     * // result == ["私", "日本語", "勉強"]
     * ```
     *
     * @param tokens 필터링할 Kuromoji 토큰 목록입니다.
     * @return 기본 품사가 명사(`名詞`)인 토큰 목록입니다.
     */
    fun filterNoun(tokens: List<Token>): List<Token> {
        return filter(tokens) { it.isNoun() }
    }
}
