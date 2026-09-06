package io.bluetape4k.tokenizer.japanese.utils

import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.tokenizer.utils.SuspendMemoized
import kotlinx.coroutines.CompletableDeferred
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.ResourceLock
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

@ResourceLock("JapaneseDictionaryProvider")
class JapaneseSuspendMemoizedInterruptionTest {

    @Test
    fun `공통 blocking bridge는 interrupt flag를 복구하고 실패한 초기화를 재시도한다`() {
        val calls = AtomicInteger()
        val started = CountDownLatch(1)
        val release = CompletableDeferred<Unit>()
        val failure = AtomicReference<Throwable?>()
        val interrupted = AtomicBoolean()
        val memoized = SuspendMemoized {
            calls.incrementAndGet()
            started.countDown()
            release.await()
            "recovered-after-interruption"
        }

        val thread = Thread({
            try {
                memoized.getBlocking()
            } catch (cause: Throwable) {
                failure.set(cause)
            } finally {
                interrupted.set(Thread.currentThread().isInterrupted)
            }
        }, "japanese-suspend-memoized-blocking-test")
        thread.start()

        try {
            started.await(5, TimeUnit.SECONDS).shouldBeTrue()
            thread.interrupt()
            thread.join(TimeUnit.SECONDS.toMillis(5))

            thread.isAlive.shouldBeFalse()
            (failure.get() is InterruptedException).shouldBeTrue()
            interrupted.get().shouldBeTrue()
            memoized.isInitialized().shouldBeFalse()

            release.complete(Unit)
            memoized.getBlocking() shouldBeEqualTo "recovered-after-interruption"
            calls.get() shouldBeEqualTo 2
        } finally {
            release.complete(Unit)
            thread.interrupt()
            thread.join(TimeUnit.SECONDS.toMillis(5))
        }
    }
}
