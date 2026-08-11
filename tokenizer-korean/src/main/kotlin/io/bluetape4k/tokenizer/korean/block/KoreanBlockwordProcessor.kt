package io.bluetape4k.tokenizer.korean.block

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.error
import io.bluetape4k.logging.trace
import io.bluetape4k.support.EMPTY_STRING
import io.bluetape4k.tokenizer.exceptions.InvalidTokenizeRequestException
import io.bluetape4k.tokenizer.exceptions.TokenizerException
import io.bluetape4k.tokenizer.korean.tokenizer.KoreanToken
import io.bluetape4k.tokenizer.korean.tokenizer.KoreanTokenizer
import io.bluetape4k.tokenizer.korean.utils.KoreanDictionaryProvider
import io.bluetape4k.tokenizer.korean.utils.KoreanPos
import io.bluetape4k.tokenizer.model.BlockwordRequest
import io.bluetape4k.tokenizer.model.BlockwordResponse
import io.bluetape4k.tokenizer.model.Severity
import io.bluetape4k.tokenizer.model.blockwordResponseOf
import io.bluetape4k.tokenizer.model.requireBlockwordTextLength
import java.util.*

/**
 * 문장에서 금칙어를 탐지하고 마스킹 결과를 생성합니다.
 *
 * ## 동작/계약
 * - 구두점 우회 패턴을 제거한 뒤 토크나이즈 결과로 금칙어를 판정한다.
 * - 마스킹 대상은 길이 2 이상, 지정 품사(`blockedPos`)이며 금칙어 사전에 존재하는 토큰이다.
 * - 처리 중 예외는 `TokenizerException`으로 감싸 재던진다.
 *
 * ```kotlin
 * val found = KoreanBlockwordProcessor.findBlockwords("미니미와 니미")
 * // found.any { it.text == "니미" } == true
 * ```
 */
object KoreanBlockwordProcessor: KLogging() {

    /**
     * 금칙어로서 마스킹 처리할 수 있는 품사
     */
    private val blockedPos = listOf(
        KoreanPos.Noun,
        KoreanPos.Adjective,
        KoreanPos.Verb,
        KoreanPos.Adverb,
        KoreanPos.Korean,
        KoreanPos.KoreanParticle,
        KoreanPos.Foreign,
        KoreanPos.Number,
        KoreanPos.Alpha,        // 영어 금칙어도 적용한다
    )

    private val punctuationProcessor = PunctuationProcessor()

    /**
     * 입력 문장에서 금칙어 토큰 목록을 반환합니다.
     *
     * ## 동작/계약
     * - 토큰화 전에 최대 입력 길이를 검증한다.
     * - 공백/빈 문자열 입력이면 빈 리스트를 반환한다.
     * - 구두점 제거 후 토큰화한 결과에서 길이 2 이상 토큰만 검사한다.
     *
     * ```kotlin
     * val tokens = KoreanBlockwordProcessor.findBlockwords("미니미와 니미")
     * // tokens.map { it.text } == ["니미"]
     * ```
     *
     * @param text 금칙어를 찾을 입력 문자열입니다.
     * @return 금칙어 사전에 걸린 [KoreanToken] list입니다. 입력이 blank이면 빈 list입니다.
     * @throws TokenizerException 처리 중 예외가 발생하면 원인을 감싸 던집니다.
     */
    fun findBlockwords(text: String): List<KoreanToken> {
        requireBlockwordTextLength(text)
        if (text.isBlank()) {
            return emptyList()
        }
        try {
            val punctuationRemoved = punctuationProcessor.removePunctuation(text)
            val blockwordDictionary = KoreanDictionaryProvider.currentBlockwordSnapshot().value
            val tokens = KoreanTokenizer.tokenize(punctuationRemoved)
            val blockWords = mutableListOf<KoreanToken>()
            tokens
                .onEach { token ->
                    log.trace {
                        "blockword candidate token. offset=${token.offset}, length=${token.length}, pos=${token.pos}"
                    }
                }
                .filter { it.length > 1 }
                .onEach { token ->
                    log.trace {
                        "try to mask block word. offset=${token.offset}, length=${token.length}, pos=${token.pos}"
                    }
                }
                .forEach { token ->
                    if (canMask(token, blockwordDictionary)) {
                        log.trace { "mask block word. offset=${token.offset}, length=${token.length}" }
                        blockWords.add(token)
                    }
                }
            return blockWords
        } catch (e: Error) {
            throw e
        } catch (e: Exception) {
            log.error(e) { "Fail to mask block word. textLength=${text.length}" }
            throw TokenizerException("Fail to mask block word. textLength=${text.length}", e)
        }
    }

    /**
     * 요청 옵션에 따라 금칙어를 마스킹한 응답을 반환합니다.
     *
     * ## 동작/계약
     * - 토큰화 전에 최대 입력 길이를 검증한다.
     * - 입력 텍스트가 비어 있으면 빈 문자열 응답을 반환한다.
     * - 요청 언어가 한국어가 아니면 `InvalidTokenizeRequestException`을 던진다.
     * - severity 조건을 만족하는 토큰 구간을 `mask` 문자열 반복값으로 치환한다.
     *
     * ```kotlin
     * val response = KoreanBlockwordProcessor.maskBlockwords(BlockwordRequest("미니미와 니미"))
     * // response.text.contains("**") == true
     * ```
     *
     * @param request 입력 문자열, locale, mask 문자열, severity를 담은 금칙어 처리 요청입니다.
     * @return 마스킹된 문자열과 발견된 금칙어를 담은 [BlockwordResponse]입니다.
     * @throws InvalidTokenizeRequestException 요청 locale이 한국어가 아니면 던집니다.
     * @throws TokenizerException 처리 중 예외가 발생하면 원인을 감싸 던집니다.
     */
    fun maskBlockwords(request: BlockwordRequest): BlockwordResponse {
        requireBlockwordTextLength(request.text)
        if (request.text.isBlank()) {
            return BlockwordResponse(request, EMPTY_STRING)
        }
        if (request.options.locale.language != Locale.KOREAN.language) {
            throw InvalidTokenizeRequestException("Invalid Language[${request.options.locale.language}], Only support Korean")
        }
        try {
            val punctuationRemoved = punctuationProcessor.removePunctuation(request.text)
            val blockwordDictionary = KoreanDictionaryProvider.currentBlockwordSnapshot().value
            val tokens = KoreanTokenizer.tokenize(punctuationRemoved)

            val maskStr = request.options.mask
            val blockWords = mutableListOf<String>()

            val tokensToMask = tokens
                .filter { !it.unknown && it.length > 1 }
                .onEach { token ->
                    log.trace {
                        "try to mask block word. offset=${token.offset}, length=${token.length}, pos=${token.pos}"
                    }
                }
                .filter { canMask(it, blockwordDictionary, request.options.severity) }

            val result = StringBuilder(punctuationRemoved).apply {
                // 멀티 문자 마스크에서도 토큰 offset이 틀어지지 않도록 뒤에서부터 치환한다.
                tokensToMask
                    .sortedByDescending { it.offset }
                    .forEach { token ->
                        log.trace { "mask block word. offset=${token.offset}, length=${token.length}" }
                        replace(
                            token.offset,
                            token.offset + token.length,
                            maskStr.repeat(token.length)
                        )
                        blockWords.add(token.text)
                    }
            }
            return blockwordResponseOf(request, result.toString(), blockWords)
        } catch (e: Error) {
            throw e
        } catch (e: Exception) {
            log.error(e) { "Fail to mask block word. textLength=${request.text.length}" }
            throw TokenizerException("Fail to mask block word. textLength=${request.text.length}", e)
        }
    }

    /**
     * [token]이 금칙어로서 마스킹되어야 하는지 판단합니다.
     *
     * 단어 자체 또는 동사의 기본형이 금칙어에 포함되어 있는지 검사합니다.
     *
     * @param token 검사할 한국어 token입니다.
     * @param severity 적용할 금칙어 심각도입니다.
     * @return [token]이 지정 심각도에서 마스킹 대상이면 `true`입니다.
     */
    private fun canMask(
        token: KoreanToken,
        blockwordDictionary: Map<Severity, Set<String>>,
        severity: Severity = Severity.DEFAULT,
    ): Boolean {
        return token.pos in blockedPos &&
                (containsBlockWord(token.text, blockwordDictionary, severity) ||
                        containsBlockWord(token.stem, blockwordDictionary, severity))
    }

    private fun containsBlockWord(
        text: String?,
        blockwordDictionary: Map<Severity, Set<String>>,
        severity: Severity = Severity.DEFAULT,
    ): Boolean {
        return text?.let { blockwordDictionary[severity]?.contains(it) == true } ?: false
    }
}
