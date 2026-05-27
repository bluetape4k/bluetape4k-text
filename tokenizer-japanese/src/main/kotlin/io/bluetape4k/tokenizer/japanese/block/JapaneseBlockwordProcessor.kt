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
 * Detects and masks blockwords in Japanese sentences using the Kuromoji IPAdic tokenizer.
 *
 * Candidate tokens are limited to nouns and verbs. When single-token matching yields
 * no results, compound-word combinations (noun + noun/verb) are also checked.
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
     * Returns tokens in the sentence that match entries in the blockword dictionary.
     *
     * Oversized input is rejected before Kuromoji is invoked.
     * Blank input returns an empty list immediately. If noun/verb token matching yields
     * no results and there are at least two tokens, compound-word matching is attempted.
     *
     * ```kotlin
     * val found = JapaneseBlockwordProcessor.findBlockwords("覚せい剤を注文できるサイトはありますか？")
     *     .map { it.surface }
     *
     * // result == ["覚せい"]
     * ```
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
                    "blockword candidate token. position=${token.position}, length=${token.surface.length}, featureCount=${token.featureCount}"
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
     * Checks adjacent token pairs (noun + noun/verb) against the blockword dictionary.
     *
     * Examples: 覚せい剤 (覚せい + 剤), 盗撮す (盗 + 撮す).
     *
     * ```kotlin
     * val request = io.bluetape4k.tokenizer.model.blockwordRequestOf("覚せい剤を注文できるサイトはありますか？")
     * val response = JapaneseBlockwordProcessor.maskBlockwords(request)
     *
     * // response.blockwordExists == true
     * ```
     */
    private fun processCompositBlockWords(tokens: List<Token>): List<Token> {
        if (tokens.size < 2) {
            return emptyList()
        }
        return tokens.zipWithNext { t1, t2 ->
            if (t1.isNoun() && t2.isNounOrVerb()) {
                val composite = t1.surface + t2.surface
                log.debug { "check blockword composite. length=${composite.length}" }
                if (isBlockword(composite)) t1 else null
            } else {
                null
            }
        }.filterNotNull()
    }

    /**
     * Replaces blockword tokens in the request text with the configured mask string.
     *
     * Oversized input is rejected before Kuromoji is invoked.
     * Blank input returns a response with an empty masked text. Each matched token surface
     * is replaced with the mask character repeated to match the token's length.
     * Processing exceptions are wrapped and rethrown as [io.bluetape4k.tokenizer.exceptions.TokenizerException].
     *
     * ```kotlin
     * val request = io.bluetape4k.tokenizer.model.blockwordRequestOf("ホモの男性を理解できない")
     * val response = JapaneseBlockwordProcessor.maskBlockwords(request)
     *
     * // response.maskedText == "**の男性を理解できない"
     * ```
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
                        "blockword candidate token. position=${token.position}, length=${token.surface.length}, featureCount=${token.featureCount}"
                    }
                }
                .filter { it.isNounOrVerb() }
                .sortedByDescending { it.position }
                .forEach { token ->
                    if (canMask(token)) {
                        log.trace { "mask block word. position=${token.position}, length=${token.surface.length}" }
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
            log.error(e) { "Fail to mask block words. textLength=${request.text.length}" }
            throw TokenizerException("Fail to mask block words. textLength=${request.text.length}", e)
        }
    }

    private fun canMask(token: Token): Boolean {
        return isBlockword(token.surface)
    }

    private fun isBlockword(text: String): Boolean {
        return JapaneseDictionaryProvider.blockWordDictionary.contains(text)
    }

    private val Token.featureCount: Int get() = allFeaturesArray.size
}
