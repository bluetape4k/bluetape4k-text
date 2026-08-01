package io.bluetape4k.text.examples.tokenizer

import com.github.pemistahl.lingua.api.Language
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeGreaterThan
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldContain
import io.bluetape4k.assertions.shouldNotContain
import io.bluetape4k.tokenizer.model.MAX_TOKENIZE_TEXT_LENGTH
import org.junit.jupiter.api.Test

class ModerationServiceExamplesTest {

    @Test
    fun `mixed language input routes tokenizers and masks keyword and blockword`() {
        val response = TextModerationService().moderate("안녕하세요 password와 비속어를 검사합니다")

        response.statusCode shouldBeEqualTo 200
        response.detectedLanguages shouldContain Language.KOREAN
        response.tokenCount.shouldBeGreaterThan(0)
        response.matches.map { it.kind } shouldBeEqualTo listOf(
            ModerationMatchKind.KEYWORD,
            ModerationMatchKind.BLOCKWORD,
        )
        response.maskedText shouldBeEqualTo "안녕하세요 ********와 ***를 검사합니다"
    }

    @Test
    fun `Japanese input is routed through Japanese tokenizer`() {
        val response = TextModerationService().moderate("秘密の文章を確認します")

        response.statusCode shouldBeEqualTo 200
        response.detectedLanguages shouldContain Language.JAPANESE
        response.tokenCount.shouldBeGreaterThan(0)
        response.matches shouldBeEqualTo emptyList()
    }

    @Test
    fun `blank and oversized inputs return sanitized errors`() {
        val service = TextModerationService()
        val blank = service.moderate("   ")
        val sentinel = "PRIVATE_MODERATION_INPUT"
        val oversizedText = sentinel.repeat((MAX_TOKENIZE_TEXT_LENGTH / sentinel.length) + 1)
        val oversized = service.moderate(oversizedText)

        blank.statusCode shouldBeEqualTo 400
        blank.error shouldContain "blank"
        blank.error shouldNotContain "   "
        oversized.statusCode shouldBeEqualTo 413
        oversized.error shouldContain oversizedText.length.toString()
        oversized.error shouldNotContain sentinel
        oversized.maskedText shouldBeEqualTo ""
    }

    @Test
    fun `unsupported script is rejected without exposing input`() {
        val response = TextModerationService().moderate("🙂🙂🙂")

        response.statusCode shouldBeEqualTo 422
        response.error shouldContain "supported language"
        response.error shouldNotContain "🙂"
        response.maskedText shouldBeEqualTo ""
    }

    @Test
    fun `rendered response contains policy summary`() {
        val output = renderTextModerationResponse(runTextModerationExample())

        output shouldContain "status=200"
        output shouldContain "KEYWORD:password"
        output shouldContain "BLOCKWORD:비속어"
        output shouldContain "masked="
        output.contains("password").shouldBeTrue()
    }
}
