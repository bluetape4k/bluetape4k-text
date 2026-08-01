package io.bluetape4k.tokenizer.japanese.block

import com.atilika.kuromoji.ipadic.Token
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.error
import io.bluetape4k.logging.trace
import io.bluetape4k.support.EMPTY_STRING
import io.bluetape4k.tokenizer.exceptions.TokenizerException
import io.bluetape4k.tokenizer.japanese.tokenizer.JapaneseTokenizer
import io.bluetape4k.tokenizer.japanese.tokenizer.isNoun
import io.bluetape4k.tokenizer.japanese.tokenizer.isNounOrVerb
import io.bluetape4k.tokenizer.japanese.utils.JapaneseDictionaryProvider
import io.bluetape4k.tokenizer.model.BlockwordRequest
import io.bluetape4k.tokenizer.model.BlockwordResponse
import io.bluetape4k.tokenizer.model.blockwordResponseOf
import io.bluetape4k.tokenizer.model.requireBlockwordTextLength

/**
 * Kuromoji IPADic 토크나이저로 일본어 문장의 금칙어를 탐지하고 마스킹합니다.
 *
 * 후보 토큰은 명사와 동사로 제한합니다. 단일 토큰 매치가 없으면 명사 + 명사/동사 복합어 조합도 검사합니다.
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
     * 공백 입력은 즉시 빈 목록을 반환합니다. 명사/동사 단일 토큰 매치가 없고 토큰이 2개 이상이면 복합어 매칭을 시도합니다.
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
        val blockwords = tokens
            .onEach { token ->
                log.trace {
                    "금칙어 후보 토큰입니다. position=${token.position}, length=${token.surface.length}, featureCount=${token.featureCount}"
                }
            }
            .filter { it.isNounOrVerb() }
            .filter { isBlockword(it.surface) }
            .toMutableList()

        if (blockwords.isEmpty() && tokens.size > 1) {
            blockwords.addAll(processCompositBlockWords(tokens))
        }

        return blockwords
    }

    /**
     * 인접 토큰 쌍(명사 + 명사/동사)을 금칙어 사전과 대조합니다.
     *
     * 예: 覚せい剤(覚せい + 剤), 盗撮す(盗 + 撮す).
     *
     * ```kotlin
     * val request = io.bluetape4k.tokenizer.model.blockwordRequestOf("覚せい剤を注文できるサイトはありますか？")
     * val response = JapaneseBlockwordProcessor.maskBlockwords(request)
     *
     * // response.blockwordExists == true
     * ```
     *
     * @param tokens 복합어 후보를 만들 Kuromoji 토큰 목록입니다.
     * @return 복합어 금칙어에 매치된 첫 번째 토큰 목록입니다.
     */
    private fun processCompositBlockWords(tokens: List<Token>): List<Token> {
        if (tokens.size < 2) {
            return emptyList()
        }
        return tokens.zipWithNext { t1, t2 ->
            if (t1.isNoun() && t2.isNounOrVerb()) {
                val composite = t1.surface + t2.surface
                log.debug { "금칙어 복합어 후보를 확인합니다. length=${composite.length}" }
                if (isBlockword(composite)) t1 else null
            } else {
                null
            }
        }.filterNotNull()
    }

    /**
     * 요청 텍스트의 금칙어 토큰을 설정된 마스크 문자열로 치환합니다.
     *
     * Kuromoji 호출 전에 길이 초과 입력을 거부합니다.
     * 공백 입력은 빈 마스킹 텍스트 응답을 반환합니다. 매치된 토큰 표면형은 토큰 길이만큼 반복한 마스크 문자로 치환합니다.
     * 처리 중 발생한 예외는 [io.bluetape4k.tokenizer.exceptions.TokenizerException]으로 감싸 다시 던집니다.
     *
     * ```kotlin
     * val request = io.bluetape4k.tokenizer.model.blockwordRequestOf("ホモの男性を理解できない")
     * val response = JapaneseBlockwordProcessor.maskBlockwords(request)
     *
     * // response.maskedText == "**の男性を理解できない"
     * ```
     *
     * @param request 원문과 마스킹 옵션을 담은 금칙어 요청입니다.
     * @return 마스킹된 텍스트와 매치된 금칙어 목록을 담은 응답입니다.
     */
    fun maskBlockwords(request: BlockwordRequest): BlockwordResponse {
        requireBlockwordTextLength(request.text)
        if (request.text.isBlank()) {
            return BlockwordResponse(request, EMPTY_STRING)
        }

        try {

            val tokens = JapaneseTokenizer.tokenize(request.text)
            var maskedText = request.text
            val maskStr = request.options.mask
            val blockwords = mutableListOf<String>()

            tokens
                .onEach { token ->
                    log.trace {
                        "금칙어 후보 토큰입니다. position=${token.position}, length=${token.surface.length}, featureCount=${token.featureCount}"
                    }
                }
                .filter { it.isNounOrVerb() }
                .sortedByDescending { it.position }
                .forEach { token ->
                    if (canMask(token)) {
                        log.trace { "금칙어를 마스킹합니다. position=${token.position}, length=${token.surface.length}" }
                        maskedText = maskedText.replaceRange(
                            token.position,
                            token.position + token.surface.length,
                            maskStr.repeat(token.surface.length)
                        )
                        blockwords.add(token.surface)
                    }
                }
            return blockwordResponseOf(request, maskedText, blockwords)
        } catch (e: Error) {
            throw e
        } catch (e: Exception) {
            log.error(e) { "금칙어 마스킹에 실패했습니다. textLength=${request.text.length}" }
            throw TokenizerException("금칙어 마스킹에 실패했습니다. textLength=${request.text.length}", e)
        }
    }

    private fun canMask(token: Token): Boolean {
        return isBlockword(token.surface)
    }

    private fun isBlockword(text: String): Boolean {
        return JapaneseDictionaryProvider.containsBlockword(text)
    }

    private val Token.featureCount: Int get() = allFeaturesArray.size
}
