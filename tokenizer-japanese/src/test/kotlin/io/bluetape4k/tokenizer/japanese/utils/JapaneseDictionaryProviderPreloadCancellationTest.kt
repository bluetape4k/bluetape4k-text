package io.bluetape4k.tokenizer.japanese.utils

import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.junit5.coroutines.runSuspendIO
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ThreadContextElement
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.ResourceLock
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.concurrent.CountDownLatch
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration.Companion.seconds

@ResourceLock("JapaneseDictionaryProvider")
class JapaneseDictionaryProviderPreloadCancellationTest {

    @Test
    fun `금칙어 loader가 읽는 중 취소되어도 public preload를 다시 호출할 수 있다`() = runSuspendIO {
        JapaneseDictionaryProvider.resetForTesting()
        val parent = Thread.currentThread().contextClassLoader
        val started = CompletableDeferred<Unit>()
        val stream = BlockingInputStream(started, "性器\n".toByteArray())
        val classLoader = BlockingResourceClassLoader(
            parent = parent,
            blockedPath = "japanesetext/block/blocks.txt",
            stream = stream,
        )

        coroutineScope {
            val job = async(Dispatchers.Default + ContextClassLoader(classLoader)) {
                JapaneseDictionaryProvider.preload()
            }

            try {
                withTimeout(5.seconds) { started.await() }

                job.cancel()
                withTimeout(5.seconds) {
                    stream.closed.await()
                    job.join()
                }
                job.isCancelled.shouldBeTrue()

                withContext(ContextClassLoader(parent)) {
                    JapaneseDictionaryProvider.preload()
                }
                JapaneseDictionaryProvider.allDictionariesInitialized().shouldBeTrue()
            } finally {
                stream.release()
                job.cancelAndJoin()
                JapaneseDictionaryProvider.resetForTesting()
            }
        }
    }

    private class BlockingResourceClassLoader(
        parent: ClassLoader,
        private val blockedPath: String,
        private val stream: InputStream,
    ): ClassLoader(parent) {

        override fun getResourceAsStream(name: String): InputStream? {
            return if (name == blockedPath) stream else super.getResourceAsStream(name)
        }
    }

    private class BlockingInputStream(
        private val started: CompletableDeferred<Unit>,
        bytes: ByteArray,
    ): InputStream() {

        private val releaseLatch = CountDownLatch(1)
        private val delegate = ByteArrayInputStream(bytes)
        private var gateOpened = false
        val closed = CompletableDeferred<Unit>()

        override fun read(): Int {
            if (!gateOpened) {
                started.complete(Unit)
                try {
                    releaseLatch.await()
                } catch (_: InterruptedException) {
                    // runInterruptible가 blocking read를 깨웠으므로 유효한 fixture를 계속 읽습니다.
                } finally {
                    gateOpened = true
                }
            }
            return delegate.read()
        }

        override fun close() {
            closed.complete(Unit)
            releaseLatch.countDown()
            delegate.close()
        }

        fun release() {
            releaseLatch.countDown()
        }
    }

    private class ContextClassLoader(
        private val classLoader: ClassLoader,
    ): ThreadContextElement<ClassLoader?>,
        AbstractCoroutineContextElement(Key) {

        companion object Key: CoroutineContext.Key<ContextClassLoader>

        override fun updateThreadContext(context: CoroutineContext): ClassLoader? {
            val previous = Thread.currentThread().contextClassLoader
            Thread.currentThread().contextClassLoader = classLoader
            return previous
        }

        override fun restoreThreadContext(context: CoroutineContext, oldState: ClassLoader?) {
            Thread.currentThread().contextClassLoader = oldState
        }
    }
}
