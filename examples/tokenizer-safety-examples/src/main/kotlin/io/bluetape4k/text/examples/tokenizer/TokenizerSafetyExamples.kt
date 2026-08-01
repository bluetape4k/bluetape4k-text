package io.bluetape4k.text.examples.tokenizer

import io.bluetape4k.tokenizer.japanese.JapaneseProcessor
import io.bluetape4k.tokenizer.korean.KoreanProcessor
import io.bluetape4k.tokenizer.model.MAX_BLOCKWORD_TEXT_LENGTH
import io.bluetape4k.tokenizer.model.MAX_TOKENIZE_TEXT_LENGTH
import io.bluetape4k.tokenizer.model.blockwordRequestOf
import java.io.Serial
import java.io.Serializable

internal enum class TokenizerLanguage {
    KOREAN,
    JAPANESE,
}

internal data class SafetyResponse(
    val status: Int,
    val body: String,
): Serializable {
    companion object {
        @Serial
        private const val serialVersionUID: Long = 1L
    }
}

internal class TokenizerSafetyHandler(
    private val koreanTokenize: (String) -> Int = { KoreanProcessor.tokenize(it).size },
    private val japaneseTokenize: (String) -> Int = { JapaneseProcessor.tokenize(it).size },
    private val koreanBlockword: (String) -> String = { KoreanProcessor.maskBlockwords(blockwordRequestOf(it)).maskedText },
    private val japaneseBlockword: (String) -> String = { JapaneseProcessor.maskBlockwords(blockwordRequestOf(it)).maskedText },
) {

    fun handleTokenize(language: TokenizerLanguage, text: String): SafetyResponse =
        validate(text, MAX_TOKENIZE_TEXT_LENGTH)?.let { it } ?: runCatching {
            val tokenCount = when (language) {
                TokenizerLanguage.KOREAN -> koreanTokenize(text)
                TokenizerLanguage.JAPANESE -> japaneseTokenize(text)
            }
            SafetyResponse(200, "tokenize accepted: language=$language, tokens=$tokenCount")
        }.getOrElse {
            SafetyResponse(500, "tokenize failed: processor error")
        }

    fun handleBlockword(language: TokenizerLanguage, text: String): SafetyResponse =
        validate(text, MAX_BLOCKWORD_TEXT_LENGTH)?.let { it } ?: runCatching {
            val maskedText = when (language) {
                TokenizerLanguage.KOREAN -> koreanBlockword(text)
                TokenizerLanguage.JAPANESE -> japaneseBlockword(text)
            }
            SafetyResponse(200, "blockword accepted: language=$language, maskedLength=${maskedText.length}")
        }.getOrElse {
            SafetyResponse(500, "blockword failed: processor error")
        }

    private fun validate(text: String, maxLength: Int): SafetyResponse? {
        if (text.length > maxLength) {
            return SafetyResponse(413, "text too long: ${text.length} chars (max $maxLength)")
        }
        if (text.isBlank()) {
            return SafetyResponse(400, "text is blank")
        }
        return null
    }
}

internal fun runTokenizerSafetyExamples(): List<SafetyResponse> {
    val handler = TokenizerSafetyHandler()
    return listOf(
        handler.handleTokenize(TokenizerLanguage.KOREAN, "안녕하세요 코틀린"),
        handler.handleTokenize(TokenizerLanguage.JAPANESE, "こんにちは Kotlin"),
        handler.handleBlockword(TokenizerLanguage.KOREAN, "안전한 문장입니다"),
        handler.handleBlockword(TokenizerLanguage.JAPANESE, "安全な文章です"),
    )
}

internal fun renderTokenizerSafetyReport(rawOversizedText: String): String {
    val response = TokenizerSafetyHandler().handleTokenize(TokenizerLanguage.KOREAN, rawOversizedText)
    return "status=${response.status}, body=${response.body}"
}

fun main() {
    runTokenizerSafetyExamples().forEach { response ->
        println("status=${response.status}, body=${response.body}")
    }
    val oversized = "sensitive-input-".repeat((MAX_TOKENIZE_TEXT_LENGTH / "sensitive-input-".length) + 1)
    println(renderTokenizerSafetyReport(oversized))
    println(renderDictionaryReloadReport(runDictionaryReloadExample()))
}
