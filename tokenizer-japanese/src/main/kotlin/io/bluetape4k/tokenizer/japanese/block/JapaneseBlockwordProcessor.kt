package io.bluetape4k.tokenizer.japanese.block

import com.atilika.kuromoji.ipadic.Token
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.error
import io.bluetape4k.logging.trace
import io.bluetape4k.support.EMPTY_STRING
import io.bluetape4k.tokenizer.exceptions.InvalidTokenizeRequestException
import io.bluetape4k.tokenizer.exceptions.TokenizerException
import io.bluetape4k.tokenizer.japanese.tokenizer.JapaneseTokenizer
import io.bluetape4k.tokenizer.japanese.tokenizer.isNoun
import io.bluetape4k.tokenizer.japanese.tokenizer.isNounOrVerb
import io.bluetape4k.tokenizer.japanese.utils.JapaneseDictionaryProvider
import io.bluetape4k.tokenizer.model.BlockwordRequest
import io.bluetape4k.tokenizer.model.BlockwordResponse
import io.bluetape4k.tokenizer.model.Severity
import io.bluetape4k.tokenizer.model.blockwordResponseOf
import io.bluetape4k.tokenizer.model.requireBlockwordTextLength
import java.util.Locale

/**
 * Kuromoji IPADic 토크나이저로 일본어 문장의 금칙어를 탐지하고 마스킹합니다.
 *
 * 후보 토큰은 명사와 동사로 제한합니다. 단일 토큰과 명사 + 명사/동사 복합어 조합을 같은 match 모델로 검사합니다.
 *
 * ```kotlin
 * val blockwords = JapaneseBlockwordProcessor
 *     .findBlockwords("ホモの男性を理解できない")
 *     .map { it.surface }
 *
 * // result == ["ホモ"]
 * ```
 */
object JapaneseBlockwordProcessor: KLogging() {

    /**
     * 문장에서 금칙어 사전에 매치되는 토큰을 반환합니다.
     *
     * Kuromoji 호출 전에 길이 초과 입력을 거부합니다.
     * 공백 입력은 즉시 빈 목록을 반환합니다. 단일 토큰과 인접 복합어 후보가 겹치면 더 긴 match를 우선합니다.
     *
     * ```kotlin
     * val found = JapaneseBlockwordProcessor.findBlockwords("覚せい剤を注文できるサイトはありますか？")
     *     .map { it.surface }
     *
     * // result == ["覚せい"]
     * ```
     *
     * @param text 금칙어를 찾을 일본어 입력 문장입니다.
     * @return 금칙어 사전에 매치된 Kuromoji 토큰 목록입니다.
     */
    fun findBlockwords(text: String): List<Token> {
        requireBlockwordTextLength(text)
        if (text.isBlank()) {
            return emptyList()
        }
        val tokens = JapaneseTokenizer.tokenize(text)
        val blockwordDictionary = JapaneseDictionaryProvider.currentBlockwordSnapshot().value
        return collectBlockwordMatches(tokens) { isBlockword(it, blockwordDictionary) }
            .map { it.token }
    }

    /**
     * 요청 텍스트의 금칙어 토큰을 설정된 마스크 문자열로 치환합니다.
     *
     * Kuromoji 호출 전에 길이 초과 입력을 거부합니다.
     * 공백 입력은 빈 마스킹 텍스트 응답을 반환합니다. 매치된 단일/복합 표면형은 match 길이만큼 반복한 마스크 문자로 치환합니다.
     * 요청 locale은 일본어만 허용하며, severity는 LOW(전체), MIDDLE(middle/high), HIGH(high) threshold로 적용합니다.
     * 처리 중 발생한 예외는 [io.bluetape4k.tokenizer.exceptions.TokenizerException]으로 감싸 다시 던집니다.
     *
     * ```kotlin
     * val options = io.bluetape4k.tokenizer.model.blockwordOptionsOf(locale = java.util.Locale.JAPANESE)
     * val request = io.bluetape4k.tokenizer.model.blockwordRequestOf("ホモの男性を理解できない", options)
     * val response = JapaneseBlockwordProcessor.maskBlockwords(request)
     *
     * // response.maskedText == "**の男性を理解できない"
     * ```
     *
     * @param request 원문과 마스킹 옵션을 담은 금칙어 요청입니다.
     * @return 마스킹된 텍스트와 매치된 금칙어 목록을 담은 응답입니다.
     * @throws InvalidTokenizeRequestException 요청 locale이 일본어가 아니면 던집니다.
     */
    fun maskBlockwords(request: BlockwordRequest): BlockwordResponse {
        requireBlockwordTextLength(request.text)
        if (request.text.isBlank()) {
            return BlockwordResponse(request, EMPTY_STRING)
        }
        validateLocale(request.options.locale)

        try {
            val blockwordDictionary = JapaneseDictionaryProvider.currentBlockwordSeveritySnapshot().value
            val tokens = JapaneseTokenizer.tokenize(request.text)
            var maskedText = request.text
            val maskStr = request.options.mask
            val blockwords = mutableListOf<String>()

            collectBlockwordMatches(tokens) {
                isBlockword(it, blockwordDictionary, request.options.severity)
            }
                .sortedByDescending { it.start }
                .forEach { match ->
                    log.trace { "금칙어를 마스킹합니다. position=${match.start}, length=${match.length}" }
                    maskedText = maskedText.replaceRange(
                        match.start,
                        match.endExclusive,
                        maskStr.repeat(match.length)
                    )
                    blockwords.add(match.surface)
                }
            return blockwordResponseOf(request, maskedText, blockwords)
        } catch (e: Error) {
            throw e
        } catch (e: Exception) {
            log.error(e) {
                "금칙어 마스킹에 실패했습니다. locale=${request.options.locale}, " +
                        "severity=${request.options.severity}, textLength=${request.text.length}"
            }
            throw TokenizerException("금칙어 마스킹에 실패했습니다. textLength=${request.text.length}", e)
        }
    }

    private fun collectBlockwordMatches(
        tokens: List<Token>,
        isBlockword: (String) -> Boolean,
    ): List<BlockwordMatch> {
        val candidates = tokens
            .onEach { token ->
                log.trace {
                    "금칙어 후보 토큰입니다. position=${token.position}, length=${token.surface.length}, featureCount=${token.featureCount}"
                }
            }
            .filter { it.isNounOrVerb() }
            .filter { isBlockword(it.surface) }
            .map { token ->
                BlockwordMatch(token, token.surface, token.position, token.position + token.surface.length)
            }
            .toMutableList()

        tokens.zipWithNext { first, second ->
            if (first.isNoun() && second.isNounOrVerb()) {
                val composite = first.surface + second.surface
                log.debug { "금칙어 복합어 후보를 확인합니다. length=${composite.length}" }
                if (isBlockword(composite)) {
                    candidates.add(
                        BlockwordMatch(
                            token = first,
                            surface = composite,
                            start = first.position,
                            endExclusive = second.position + second.surface.length,
                        )
                    )
                }
            }
        }

        return candidates
            .sortedWith(compareBy<BlockwordMatch> { it.start }.thenByDescending { it.length })
            .fold(mutableListOf()) { matches, candidate ->
                if (matches.none { it.overlaps(candidate) }) {
                    matches.add(candidate)
                }
                matches
            }
    }

    private fun isBlockword(
        text: String,
        blockwordDictionary: Map<Severity, Set<String>>,
        severity: Severity,
    ): Boolean = blockwordDictionary[severity].orEmpty().contains(text)

    private fun isBlockword(text: String, blockwordDictionary: Set<String>): Boolean =
        blockwordDictionary.contains(text)

    private fun validateLocale(locale: Locale) {
        if (locale.language != Locale.JAPANESE.language) {
            throw InvalidTokenizeRequestException(
                "Invalid Language[${locale.language}], Only support Japanese"
            )
        }
    }

    private val Token.featureCount: Int get() = allFeaturesArray.size

    private data class BlockwordMatch(
        val token: Token,
        val surface: String,
        val start: Int,
        val endExclusive: Int,
    ) {
        val length: Int get() = endExclusive - start

        fun overlaps(other: BlockwordMatch): Boolean =
            start < other.endExclusive && other.start < endExclusive
    }
}
