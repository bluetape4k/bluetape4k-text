package io.bluetape4k.tokenizer.japanese

import com.atilika.kuromoji.ipadic.Token
import io.bluetape4k.logging.KLogging
import io.bluetape4k.tokenizer.japanese.block.JapaneseBlockwordProcessor
import io.bluetape4k.tokenizer.japanese.tokenizer.JapaneseTokenizer
import io.bluetape4k.tokenizer.japanese.utils.JapaneseDictionaryProvider
import io.bluetape4k.tokenizer.model.BlockwordRequest
import io.bluetape4k.tokenizer.model.BlockwordResponse
import io.bluetape4k.tokenizer.model.Severity
import io.bluetape4k.tokenizer.model.requireTokenizeTextLength

/**
 * 일본어 형태소 분석과 금칙어 탐지/마스킹을 제공하는 facade입니다.
 *
 * 토큰화는 [JapaneseTokenizer]에 위임하고, 금칙어 작업은 [JapaneseBlockwordProcessor]와
 * [JapaneseDictionaryProvider]에 위임합니다.
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
     * 일본어 금칙어 사전을 호출 코루틴을 차단하지 않고 미리 로드합니다.
     *
     * 애플리케이션 startup/readiness 단계에서 호출하면 첫 동기 facade 조회의 IO blocking을
     * 요청 경로 밖으로 이동할 수 있습니다. 실제 loader lifecycle과 취소·재시도 규칙은
     * [JapaneseDictionaryProvider]가 소유합니다.
     */
    suspend fun preload() {
        JapaneseDictionaryProvider.preload()
    }

    /**
     * 입력 문장을 형태소 토큰 목록으로 분석합니다.
     *
     * Kuromoji 호출 전에 `MAX_TOKENIZE_TEXT_LENGTH`를 초과하는 입력을 거부합니다.
     *
     * ```kotlin
     * val tokens = JapaneseProcessor.tokenize("お寿司が食べたい。")
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
        return JapaneseTokenizer.tokenize(text)
    }

    /**
     * 토큰 목록에서 [predicate]를 만족하는 토큰만 반환합니다.
     *
     * ```kotlin
     * val tokens = JapaneseProcessor.tokenize("お寿司が食べたい。")
     * val nouns = JapaneseProcessor.filter(tokens) { it.isNoun() }.map { it.surface }
     *
     * // result == ["寿司"]
     * ```
     *
     * @param tokens 필터링할 Kuromoji 토큰 목록입니다.
     * @param predicate 유지할 토큰을 판정하는 조건 함수입니다.
     * @return 조건을 만족하는 토큰 목록입니다.
     */
    fun filter(tokens: List<Token>, predicate: (Token) -> Boolean): List<Token> {
        return JapaneseTokenizer.filter(tokens, predicate)
    }

    /**
     * 토큰 목록에서 명사(`名詞`) 토큰만 반환합니다.
     *
     * ```kotlin
     * val tokens = JapaneseProcessor.tokenize("私は、日本語の勉強をしています。")
     * val nouns = JapaneseProcessor.filterNoun(tokens).map { it.surface }
     *
     * // result == ["私", "日本語", "勉強"]
     * ```
     *
     * @param tokens 필터링할 Kuromoji 토큰 목록입니다.
     * @return 기본 품사가 명사(`名詞`)인 토큰 목록입니다.
     */
    fun filterNoun(tokens: List<Token>): List<Token> {
        return JapaneseTokenizer.filterNoun(tokens)
    }

    /**
     * 문장에서 금칙어 사전에 등록된 토큰을 찾습니다.
     *
     * 단일 토큰 매치가 없으면 명사+명사/동사 조합의 복합어 매칭으로 보정합니다.
     *
     * ```kotlin
     * val blockwords = JapaneseProcessor.findBlockwords("ホモの男性を理解できない").map { it.surface }
     *
     * // result == ["ホモ"]
     * ```
     *
     * @param text 금칙어를 찾을 일본어 입력 문장입니다.
     * @return 금칙어 사전에 매치된 Kuromoji 토큰 목록입니다.
     */
    fun findBlockwords(text: String): List<Token> {
        return JapaneseBlockwordProcessor.findBlockwords(text)
    }

    /**
     * 문장의 금칙어 토큰을 요청 옵션의 마스크 문자열로 치환합니다.
     * 요청 locale은 `Locale.JAPANESE`만 허용하고, severity는 일본어 사전의 cumulative threshold로 적용합니다.
     *
     * ```kotlin
     * val options = io.bluetape4k.tokenizer.model.blockwordOptionsOf(locale = java.util.Locale.JAPANESE)
     * val request = io.bluetape4k.tokenizer.model.blockwordRequestOf("ホモの男性を理解できない", options)
     * val response = JapaneseProcessor.maskBlockwords(request)
     *
     * // response.maskedText == "**の男性を理解できない"
     * ```
     *
     * @param request 원문과 마스킹 옵션을 담은 금칙어 요청입니다.
     * @return 마스킹된 텍스트와 매치된 금칙어 목록을 담은 응답입니다.
     */
    fun maskBlockwords(request: BlockwordRequest): BlockwordResponse {
        return JapaneseBlockwordProcessor.maskBlockwords(request)
    }

    /**
     * 인메모리 금칙어 사전에 단어를 추가합니다.
     *
     * ```kotlin
     * JapaneseProcessor.addBlockwords(listOf("東京"))
     * val found = JapaneseProcessor.findBlockwords("これは東京です").map { it.surface }
     *
     * // result == ["東京"]
     * ```
     *
     * @param words 추가할 금칙어 단어 목록입니다.
     */
    fun addBlockwords(words: List<String>) {
        JapaneseDictionaryProvider.addBlockwords(words)
    }

    /** 지정한 severity tier에 인메모리 금칙어를 추가합니다.
     *
     * @param words 추가할 금칙어 단어 목록입니다.
     * @param severity 추가할 severity tier입니다.
     */
    fun addBlockwords(words: List<String>, severity: Severity) {
        JapaneseDictionaryProvider.addBlockwords(words, severity)
    }

    /**
     * 인메모리 금칙어 사전에서 단어를 제거합니다. 등록되지 않은 단어는 무시합니다.
     *
     * ```kotlin
     * JapaneseProcessor.addBlockwords(listOf("東京"))
     * JapaneseProcessor.removeBlockwords(listOf("東京"))
     * val found = JapaneseProcessor.findBlockwords("これは東京です")
     *
     * // result == []
     * ```
     *
     * @param words 제거할 금칙어 단어 목록입니다.
     */
    fun removeBlockwords(words: List<String>) {
        JapaneseDictionaryProvider.removeBlockwords(words)
    }

    /** 지정한 severity tier에서 인메모리 금칙어를 제거합니다.
     *
     * @param words 제거할 금칙어 단어 목록입니다.
     * @param severity 제거할 severity tier입니다.
     */
    fun removeBlockwords(words: List<String>, severity: Severity) {
        JapaneseDictionaryProvider.removeBlockwords(words, severity)
    }

    /**
     * 인메모리 금칙어 사전을 비웁니다. 원본 리소스 파일은 삭제하지 않습니다.
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
