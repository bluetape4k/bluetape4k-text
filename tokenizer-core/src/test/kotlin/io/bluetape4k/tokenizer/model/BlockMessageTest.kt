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

class BlockMessageTest: AbstractCoreTest() {

    companion object: KLogging()

    private fun newRequest(severity: Severity = Severity.MIDDLE): BlockwordRequest {
        return blockwordRequestOf(
            faker.lorem().paragraph(8),
            BlockwordOptions(severity = severity)
        )
    }

    @Test
    fun `create request with empty text`() {
        assertFailsWith<IllegalArgumentException> {
            blockwordRequestOf("")
        }
    }

    @Test
    fun `create request with space`() {
        assertFailsWith<IllegalArgumentException> {
            blockwordRequestOf(" ")
        }
    }

    @Test
    fun `create request with white spacet`() {
        assertFailsWith<IllegalArgumentException> {
            blockwordRequestOf("\t")
        }
    }

    @Test
    fun `blockword request rejects oversized text without leaking raw input`() {
        val rawText = "private-blockword-value-".repeat((MAX_BLOCKWORD_TEXT_LENGTH / 24) + 1)
        val actualLength = rawText.length.toString()

        assertSanitizedOversizedRequestFailure(rawText, actualLength, "private-blockword-value") {
            blockwordRequestOf(rawText)
        }

        assertSanitizedOversizedRequestFailure(rawText, actualLength, "private-blockword-value") {
            BlockwordRequest(rawText)
        }

        val json = mapper.writeAsString(mapOf("text" to rawText, "options" to BlockwordOptions.DEFAULT)).shouldNotBeNull()
        assertSanitizedOversizedJsonBindingFailure(rawText, actualLength, "private-blockword-value") {
            mapper.readValue<BlockwordRequest>(json)
        }
    }

    @Test
    fun `blockword request rejects oversized whitespace before blank scan`() {
        val rawText = " ".repeat(MAX_BLOCKWORD_TEXT_LENGTH + 1)
        val actualLength = rawText.length.toString()

        assertSanitizedOversizedRequestFailure(rawText, actualLength) {
            blockwordRequestOf(rawText)
        }

        assertSanitizedOversizedRequestFailure(rawText, actualLength) {
            BlockwordRequest(rawText)
        }

        val json = mapper.writeAsString(mapOf("text" to rawText, "options" to BlockwordOptions.DEFAULT)).shouldNotBeNull()
        assertSanitizedOversizedJsonBindingFailure(rawText, actualLength) {
            mapper.readValue<BlockwordRequest>(json)
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
        message shouldContain MAX_BLOCKWORD_TEXT_LENGTH.toString()
        message shouldNotContain rawText
        rawSentinel?.let { message shouldNotContain it }
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `convert request to json`() {
        val expected = newRequest()
        val jsonText = mapper.writeAsString(expected).shouldNotBeNull()
        val actual = mapper.readValue<BlockwordRequest>(jsonText)

        actual shouldBeEqualTo expected
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `convert response to json`() {
        val request = newRequest()
        val expected = BlockwordResponse(request, "Masked 문자열", listOf("욕설", "비속어"))

        val jsonText = mapper.writeAsString(expected).shouldNotBeNull()
        val actual = mapper.readValue<BlockwordResponse>(jsonText)

        actual shouldBeEqualTo expected
    }
}
