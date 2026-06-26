package io.bluetape4k.text.examples.tokenizer

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldContain
import io.bluetape4k.assertions.shouldNotContain
import io.bluetape4k.tokenizer.model.MAX_BLOCKWORD_TEXT_LENGTH
import io.bluetape4k.tokenizer.model.MAX_TOKENIZE_TEXT_LENGTH
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicInteger

class TokenizerSafetyExamplesTest {

    @Test
    fun `accepted Korean and Japanese requests call processors`() {
        val calls = AtomicInteger()
        val handler = TokenizerSafetyHandler(
            koreanTokenize = { calls.incrementAndGet(); 3 },
            japaneseTokenize = { calls.incrementAndGet(); 2 },
            koreanBlockword = { calls.incrementAndGet(); "clean" },
            japaneseBlockword = { calls.incrementAndGet(); "clean" },
        )

        handler.handleTokenize(TokenizerLanguage.KOREAN, "안녕하세요").status shouldBeEqualTo 200
        handler.handleTokenize(TokenizerLanguage.JAPANESE, "こんにちは").status shouldBeEqualTo 200
        handler.handleBlockword(TokenizerLanguage.KOREAN, "안전한 문장").status shouldBeEqualTo 200
        handler.handleBlockword(TokenizerLanguage.JAPANESE, "安全な文").status shouldBeEqualTo 200
        calls.get() shouldBeEqualTo 4
    }

    @Test
    fun `oversized tokenize request maps to 413 before processor calls`() {
        val calls = AtomicInteger()
        val rawText = "SENSITIVE_RAW_INPUT_".repeat((MAX_TOKENIZE_TEXT_LENGTH / 20) + 1)
        val handler = TokenizerSafetyHandler(koreanTokenize = { calls.incrementAndGet(); 1 })

        val response = handler.handleTokenize(TokenizerLanguage.KOREAN, rawText)

        response.status shouldBeEqualTo 413
        response.body shouldContain rawText.length.toString()
        response.body shouldContain MAX_TOKENIZE_TEXT_LENGTH.toString()
        response.body shouldNotContain rawText
        response.body shouldNotContain "SENSITIVE_RAW_INPUT"
        calls.get() shouldBeEqualTo 0
    }

    @Test
    fun `blank tokenize request maps to 400 without raw body echo`() {
        val response = TokenizerSafetyHandler().handleTokenize(TokenizerLanguage.KOREAN, "   ")

        response.status shouldBeEqualTo 400
        response.body shouldContain "blank"
        response.body shouldNotContain "   "
    }

    @Test
    fun `tokenize processor failure maps to sanitized 500`() {
        val sentinel = "TOKENIZER_FAILURE_SENTINEL"
        val handler = TokenizerSafetyHandler(
            koreanTokenize = { throw IllegalStateException(sentinel) },
        )

        val response = handler.handleTokenize(TokenizerLanguage.KOREAN, "안녕하세요")

        response.status shouldBeEqualTo 500
        response.body shouldContain "processor error"
        response.body shouldNotContain sentinel
    }

    @Test
    fun `oversized blockword request maps to 413 before processor calls`() {
        val calls = AtomicInteger()
        val rawText = "SENSITIVE_BLOCK_INPUT_".repeat((MAX_BLOCKWORD_TEXT_LENGTH / 22) + 1)
        val handler = TokenizerSafetyHandler(koreanBlockword = { calls.incrementAndGet(); "clean" })

        val response = handler.handleBlockword(TokenizerLanguage.KOREAN, rawText)

        response.status shouldBeEqualTo 413
        response.body shouldContain rawText.length.toString()
        response.body shouldContain MAX_BLOCKWORD_TEXT_LENGTH.toString()
        response.body shouldNotContain rawText
        response.body shouldNotContain "SENSITIVE_BLOCK_INPUT"
        calls.get() shouldBeEqualTo 0
    }

    @Test
    fun `blockword processor failure maps to sanitized 500`() {
        val sentinel = "BLOCKWORD_FAILURE_SENTINEL"
        val handler = TokenizerSafetyHandler(
            koreanBlockword = { throw IllegalStateException(sentinel) },
        )

        val response = handler.handleBlockword(TokenizerLanguage.KOREAN, "안전한 문장")

        response.status shouldBeEqualTo 500
        response.body shouldContain "processor error"
        response.body shouldNotContain sentinel
    }

    @Test
    fun `sample output omits sensitive oversized input while preserving metadata`() {
        val sentinel = "SENSITIVE_OUTPUT_INPUT_"
        val rawText = sentinel.repeat((MAX_TOKENIZE_TEXT_LENGTH / sentinel.length) + 1)

        val output = renderTokenizerSafetyReport(rawText)

        output shouldContain "413"
        output shouldContain rawText.length.toString()
        output shouldContain MAX_TOKENIZE_TEXT_LENGTH.toString()
        output shouldNotContain sentinel
        output shouldNotContain rawText
    }
}
