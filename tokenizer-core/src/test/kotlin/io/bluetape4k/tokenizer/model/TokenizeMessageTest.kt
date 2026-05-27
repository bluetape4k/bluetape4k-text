package io.bluetape4k.tokenizer.model

import io.bluetape4k.jackson3.writeAsString
import io.bluetape4k.logging.KLogging
import io.bluetape4k.tokenizer.AbstractCoreTest
import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldContain
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.assertions.shouldNotContain
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import tools.jackson.module.kotlin.readValue

class TokenizeMessageTest: AbstractCoreTest() {

    companion object: KLogging()

    private fun newRequest(): TokenizeRequest = TokenizeRequest(
        text = faker.lorem().paragraph(8),
        options = TokenizeOptions.DEFAULT,
    )

    @Test
    fun `create request with empty text`() {
        assertFailsWith<IllegalArgumentException> {
            tokenizeRequestOf(text = "")
        }
    }

    @Test
    fun `create request with space`() {
        assertFailsWith<IllegalArgumentException> {
            tokenizeRequestOf(text = " ")
        }
    }

    @Test
    fun `create request with whitespace`() {
        assertFailsWith<IllegalArgumentException> {
            tokenizeRequestOf(text = "\t")
        }
    }

    @Test
    fun `tokenize request rejects oversized text without leaking raw input`() {
        val rawText = "private-token-value-".repeat((MAX_TOKENIZE_TEXT_LENGTH / 20) + 1)
        val actualLength = rawText.length.toString()

        assertSanitizedOversizedRequestFailure(rawText, actualLength, "private-token-value") {
            tokenizeRequestOf(text = rawText)
        }

        assertSanitizedOversizedRequestFailure(rawText, actualLength, "private-token-value") {
            TokenizeRequest(text = rawText)
        }

        val json = mapper.writeAsString(mapOf("text" to rawText, "options" to TokenizeOptions.DEFAULT)).shouldNotBeNull()
        assertSanitizedOversizedJsonBindingFailure(rawText, actualLength, "private-token-value") {
            mapper.readValue<TokenizeRequest>(json)
        }
    }

    @Test
    fun `tokenize request rejects oversized whitespace before blank scan`() {
        val rawText = " ".repeat(MAX_TOKENIZE_TEXT_LENGTH + 1)
        val actualLength = rawText.length.toString()

        assertSanitizedOversizedRequestFailure(rawText, actualLength) {
            tokenizeRequestOf(text = rawText)
        }

        assertSanitizedOversizedRequestFailure(rawText, actualLength) {
            TokenizeRequest(text = rawText)
        }

        val json = mapper.writeAsString(mapOf("text" to rawText, "options" to TokenizeOptions.DEFAULT)).shouldNotBeNull()
        assertSanitizedOversizedJsonBindingFailure(rawText, actualLength) {
            mapper.readValue<TokenizeRequest>(json)
        }
    }

    private fun assertSanitizedOversizedRequestFailure(
        rawText: String,
        actualLength: String,
        rawSentinel: String? = null,
        block: () -> Unit,
    ) {
        val exception = assertFailsWith<IllegalArgumentException> {
            block()
        }
        assertSanitizedOversizedMessage(exception.message.orEmpty(), rawText, actualLength, rawSentinel)
    }

    private fun assertSanitizedOversizedJsonBindingFailure(
        rawText: String,
        actualLength: String,
        rawSentinel: String? = null,
        block: () -> Unit,
    ) {
        val exception = assertFailsWith<Exception> {
            block()
        }
        assertSanitizedOversizedMessage(exception.message.orEmpty(), rawText, actualLength, rawSentinel)
    }

    private fun assertSanitizedOversizedMessage(
        message: String,
        rawText: String,
        actualLength: String,
        rawSentinel: String? = null,
    ) {
        message shouldContain "text too long"
        message shouldContain actualLength
        message shouldContain MAX_TOKENIZE_TEXT_LENGTH.toString()
        message shouldNotContain rawText
        rawSentinel?.let { message shouldNotContain it }
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `convert request to json`() {
        val expected = newRequest()
        val json = mapper.writeAsString(expected).shouldNotBeNull()
        val actual = mapper.readValue<TokenizeRequest>(json)

        actual shouldBeEqualTo expected
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `convert response to json`() {
        val request = newRequest()
        val expected = tokenizeResponseOf(request.text, listOf("토큰1", "토큰2"))

        val json = mapper.writeAsString(expected).shouldNotBeNull()
        val actual = mapper.readValue<TokenizeResponse>(json)

        actual shouldBeEqualTo expected
    }
}
