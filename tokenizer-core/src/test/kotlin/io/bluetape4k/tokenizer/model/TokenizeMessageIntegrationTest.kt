package io.bluetape4k.tokenizer.model

import io.bluetape4k.logging.KLogging
import io.bluetape4k.tokenizer.AbstractCoreTest
import io.bluetape4k.assertions.shouldBeEqualTo
import org.junit.jupiter.api.Test
import java.util.*

class TokenizeMessageIntegrationTest: AbstractCoreTest() {

    companion object: KLogging()

    @Test
    fun `complete tokenize workflow`() {
        // 1. Option 생성
        val options = TokenizeOptions(Locale.KOREAN)

        // 2. Request 생성
        val request =
            tokenizeRequestOf(
                text = "한글 텍스트",
                options = options,
            )

        // 3. Request 검증
        request.text shouldBeEqualTo "한글 텍스트"
        request.options.locale shouldBeEqualTo Locale.KOREAN

        // 4. Response 생성
        val response =
            tokenizeResponseOf(
                text = request.text,
                tokens = listOf("한글", "텍스트"),
            )

        // 5. Response 검증
        response.text shouldBeEqualTo "한글 텍스트"
        response.tokens shouldBeEqualTo listOf("한글", "텍스트")
    }

    @Test
    fun `tokenize request with default options`() {
        val request = tokenizeRequestOf("test")

        request.text shouldBeEqualTo "test"
        request.options.locale shouldBeEqualTo Locale.KOREAN
    }

    @Test
    fun `tokenize response with empty tokens`() {
        val response = tokenizeResponseOf("test")

        response.text shouldBeEqualTo "test"
        response.tokens shouldBeEqualTo emptyList()
    }
}
