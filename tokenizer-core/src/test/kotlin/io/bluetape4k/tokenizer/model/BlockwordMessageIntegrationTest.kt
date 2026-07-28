package io.bluetape4k.tokenizer.model

import io.bluetape4k.logging.KLogging
import io.bluetape4k.tokenizer.AbstractCoreTest
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeTrue
import org.junit.jupiter.api.Test

class BlockwordMessageIntegrationTest: AbstractCoreTest() {

    companion object: KLogging()

    @Test
    fun `complete blockword workflow`() {
        // 1. Option 생성
        val options =
            blockwordOptionsOf(
                mask = "*",
                severity = Severity.MIDDLE,
            )

        // 2. Request 생성
        val request =
            blockwordRequestOf(
                text = "This is a test message",
                options = options,
            )

        // 3. Request 검증
        request.text shouldBeEqualTo "This is a test message"
        request.options.mask shouldBeEqualTo "*"
        request.options.severity shouldBeEqualTo Severity.MIDDLE

        // 4. Response 생성
        val response =
            blockwordResponseOf(
                request = request,
                maskedText = "This is a **** message",
                blockWords = listOf("test"),
            )

        // 5. Response 검증
        response.request shouldBeEqualTo request
        response.maskedText shouldBeEqualTo "This is a **** message"
        response.blockWords shouldBeEqualTo listOf("test")
        response.blockwordExists.shouldBeTrue()
    }

    @Test
    fun `blockword request with default options`() {
        val request = blockwordRequestOf("test message")

        request.text shouldBeEqualTo "test message"
        request.options shouldBeEqualTo BlockwordOptions.DEFAULT
    }

    @Test
    fun `blockword response with no blocked words`() {
        val request = blockwordRequestOf("clean message")
        val response =
            blockwordResponseOf(
                request = request,
                maskedText = "clean message",
                blockWords = emptyList(),
            )

        response.blockwordExists.shouldBeFalse()
    }
}
