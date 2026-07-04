package io.bluetape4k.tokenizer.exceptions

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldBeSameInstanceAs
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.tokenizer.AbstractCoreTest
import org.junit.jupiter.api.Test

class TokenizerExceptionTest: AbstractCoreTest() {

    @Test
    fun tokenizerExceptionConstructorsPreserveMessageAndCause() {
        val cause = IllegalArgumentException("bad input")

        TokenizerException().message.shouldBeNull()
        TokenizerException("tokenize failed").message shouldBeEqualTo "tokenize failed"
        TokenizerException("wrapped", cause).also {
            it.message shouldBeEqualTo "wrapped"
            it.cause shouldBeSameInstanceAs cause
        }
        TokenizerException(cause).cause shouldBeSameInstanceAs cause
    }

    @Test
    fun invalidTokenizeRequestExceptionConstructorsPreserveHierarchyMessageAndCause() {
        val cause = IllegalStateException("bad request")

        InvalidTokenizeRequestException().also {
            it.message.shouldBeNull()
            (it is TokenizerException).shouldBeTrue()
        }
        InvalidTokenizeRequestException("text is blank").message shouldBeEqualTo "text is blank"
        InvalidTokenizeRequestException("wrapped", cause).also {
            it.message shouldBeEqualTo "wrapped"
            it.cause shouldBeSameInstanceAs cause
        }
        InvalidTokenizeRequestException(cause).cause shouldBeSameInstanceAs cause
    }
}
