package io.bluetape4k.tokenizer.utils

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.junit5.coroutines.runSuspendIO
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.awaitCancellation
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.ResourceLock
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

@ResourceLock("SuspendMemoized")
class SuspendMemoizedTest {

    @Test
    fun `동시 호출은 성공 초기화를 한 번만 실행한다`() = runSuspendIO {
        val calls = AtomicInteger()
        val started = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val memoized = SuspendMemoized {
            calls.incrementAndGet()
            started.complete(Unit)
            release.await()
            "loaded"
        }

        val first = async(Dispatchers.Default) { memoized.get() }
        started.await()
        val rest = (1..15).map { async(Dispatchers.Default) { memoized.get() } }
        release.complete(Unit)

        (listOf(first) + rest).awaitAll().forEach { it shouldBeEqualTo "loaded" }
        calls.get() shouldBeEqualTo 1
        memoized.isInitialized().shouldBeTrue()
    }

    @Test
    fun `취소와 실패는 cache하지 않아 다음 호출에서 재시도한다`() = runSuspendIO {
        val cancellationCalls = AtomicInteger()
        val cancellationStarted = CompletableDeferred<Unit>()
        val cancellable = SuspendMemoized {
            if (cancellationCalls.incrementAndGet() == 1) {
                cancellationStarted.complete(Unit)
                awaitCancellation()
            }
            "recovered-after-job-cancellation"
        }

        val cancelled = async(Dispatchers.Default) { cancellable.get() }
        cancellationStarted.await()
        cancelled.cancel()
        cancelled.join()
        cancelled.isCancelled.shouldBeTrue()
        cancellable.get() shouldBeEqualTo "recovered-after-job-cancellation"
        cancellationCalls.get() shouldBeEqualTo 2

        val retryCalls = AtomicInteger()
        val retryable = SuspendMemoized {
            if (retryCalls.incrementAndGet() == 1) {
                throw CancellationException("cancelled initialization")
            }
            "recovered"
        }
        assertFailsWith<CancellationException> { retryable.get() }
        retryable.get() shouldBeEqualTo "recovered"
        retryCalls.get() shouldBeEqualTo 2

        val failureCalls = AtomicInteger()
        val failureRetryable = SuspendMemoized {
            if (failureCalls.incrementAndGet() == 1) {
                error("failed initialization")
            }
            "recovered-after-failure"
        }
        assertFailsWith<IllegalStateException> { failureRetryable.get() }
        failureRetryable.get() shouldBeEqualTo "recovered-after-failure"
        failureCalls.get() shouldBeEqualTo 2
    }

    @Test
    fun `blocking interruption은 flag를 복구하고 다음 호출을 재시도한다`() {
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

        // 실제 blocking bridge의 caller thread interrupt 계약은 virtual-time으로 검증할 수 없습니다.
        val thread = Thread({
            try {
                memoized.getBlocking()
            } catch (cause: Throwable) {
                failure.set(cause)
            } finally {
                interrupted.set(Thread.currentThread().isInterrupted)
            }
        }, "suspend-memoized-blocking-test")
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

    @Test
    fun `clear는 성공 값을 제거하고 다음 호출을 새로 초기화한다`() = runSuspendIO {
        val calls = AtomicInteger()
        val memoized = SuspendMemoized { calls.incrementAndGet() }

        memoized.get() shouldBeEqualTo 1
        memoized.isInitialized().shouldBeTrue()
        memoized.clear()
        memoized.isInitialized().shouldBeFalse()
        memoized.get() shouldBeEqualTo 2
        calls.get() shouldBeEqualTo 2
    }
}
