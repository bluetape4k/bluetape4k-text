package io.bluetape4k.tokenizer.korean.tokenizer

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.tokenizer.korean.block.KoreanBlockwordProcessor
import io.bluetape4k.tokenizer.korean.utils.KoreanDictionaryProvider
import io.bluetape4k.tokenizer.model.BlockwordRequest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.ResourceLock

@ResourceLock("KoreanDictionaryProvider")
class TokenizerCancellationContractTest {

    @BeforeEach
    fun resetBefore() = runSuspendIO {
        KoreanDictionaryProvider.resetForTesting()
    }

    @AfterEach
    fun resetAfter() = runSuspendIO {
        Thread.interrupted()
        KoreanDictionaryProvider.resetForTesting()
    }

    @Test
    fun `KoreanTokenizer preserves interrupted dictionary load`() {
        assertInterrupted { KoreanTokenizer.tokenize("가") }
    }

    @Test
    fun `NounTokenizer preserves interrupted dictionary load`() {
        assertInterrupted { NounTokenizer.tokenize("가") }
    }

    @Test
    fun `blockword finder preserves interrupted dictionary load`() {
        assertInterrupted { KoreanBlockwordProcessor.findBlockwords("가") }
    }

    @Test
    fun `blockword masker preserves interrupted dictionary load`() {
        assertInterrupted { KoreanBlockwordProcessor.maskBlockwords(BlockwordRequest("가")) }
    }

    private fun assertInterrupted(action: () -> Any?) {
        try {
            Thread.currentThread().interrupt()
            assertFailsWith<InterruptedException> { action() }
            Thread.currentThread().isInterrupted.shouldBeTrue()
        } finally {
            Thread.interrupted()
        }
    }
}
