package io.bluetape4k.tokenizer.utils

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldNotBeEmpty
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ThreadContextElement
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Test
import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration.Companion.seconds

class DictionaryProviderTest {

    companion object: KLoggingChannel() {
        private const val BASE_PATH = "dictionary"
        private const val NOUN_PATH = "$BASE_PATH/noun/nouns.txt"
        private const val FOREIGN_PATH = "$BASE_PATH/noun/foreign.txt"
        private const val FREQ_PATH = "$BASE_PATH/freq/entity-freq.txt.gz"
        private const val BLOCK_PATH = "$BASE_PATH/block/block.txt"
        private const val FIRST_PATH = "dictionary/test/first.txt"
        private const val SECOND_PATH = "dictionary/test/second.txt"
        private const val MISSING_PATH = "dictionary/test/missing.txt"
        private const val SLOW_PATH = "dictionary/test/slow.txt"
        private const val FAILING_PATH = "dictionary/test/failing.txt"
        private const val SIBLING_PATH = "dictionary/test/sibling.txt"
    }

    @Test
    fun `명사 사전 로드하기 as CharArraySet`() = runSuspendIO {
        val dictionary: CharArraySet = DictionaryProvider.readWords(NOUN_PATH, FOREIGN_PATH)

        dictionary.contains("각광").shouldBeTrue()
        dictionary.contains("없는명사다").shouldBeFalse()
    }

    @Test
    fun `명사 사전 로드하기 as Set`() = runSuspendIO {
        val dictionary: MutableSet<String> = DictionaryProvider.readWordsAsSet(NOUN_PATH, FOREIGN_PATH)

        dictionary.contains("각광").shouldBeTrue()
        dictionary.contains("없는명사다").shouldBeFalse()
    }

    @Test
    fun `readWordsAsSequence eagerly closes the resource before returning`() = runSuspendIO {
        val stream = TrackingInputStream("first\nsecond\n".byteInputStream())
        val classLoader = ResourceClassLoader(mapOf(FIRST_PATH to { stream }))

        val words = withContext(ContextClassLoader(classLoader)) {
            DictionaryProvider.readWordsAsSequence(FIRST_PATH)
        }

        stream.closed.get().shouldBeTrue()
        words.take(1).toList() shouldBeEqualTo listOf("first")
    }

    @Test
    fun `없는 파일 로드하면 예외가 발생한다`() = runSuspendIO {
        assertFailsWith<IllegalStateException> {
            DictionaryProvider.readWords("$BASE_PATH/noun/non-exists.txt")
        }
    }

    @Test
    fun `압축된 파일 로드하기`() {
        val dictionary: Map<CharSequence, Float> = DictionaryProvider.readWordFreqs(FREQ_PATH)
        dictionary.shouldNotBeEmpty()
    }

    @Test
    fun `금칙어 사전 로드하기`() = runSuspendIO {
        val dictionary: CharArraySet = DictionaryProvider.readWords(BLOCK_PATH)

        dictionary.contains("씨불").shouldBeTrue()
        dictionary.contains("씨발").shouldBeTrue()
        dictionary.contains("히로뽕").shouldBeTrue()

        dictionary.contains("하늘").shouldBeFalse()
    }

    @Test
    fun `여러 dictionary resource를 모두 읽고 stream을 닫는다`() = runSuspendIO {
        val first = TrackingInputStream("first\n".byteInputStream())
        val second = TrackingInputStream("second\n".byteInputStream())
        val classLoader = ResourceClassLoader(
            mapOf(
                FIRST_PATH to { first },
                SECOND_PATH to { second },
            )
        )

        val words = withContext(ContextClassLoader(classLoader)) {
            DictionaryProvider.readWordsAsSet(FIRST_PATH, SECOND_PATH)
        }

        words shouldBeEqualTo setOf("first", "second")
        first.closed.get().shouldBeTrue()
        second.closed.get().shouldBeTrue()
    }

    @Test
    fun `여러 dictionary resource가 실제로 병렬 시작된다`() = runSuspendIO {
        val firstStarted = CompletableDeferred<Unit>()
        val secondStarted = CompletableDeferred<Unit>()
        val release = CountDownLatch(1)
        val first = BarrierInputStream(firstStarted, release)
        val second = BarrierInputStream(secondStarted, release)
        val classLoader = ResourceClassLoader(
            mapOf(
                FIRST_PATH to { first },
                SECOND_PATH to { second },
            )
        )
        val job = async(Dispatchers.Default + ContextClassLoader(classLoader)) {
            DictionaryProvider.readWordsAsSet(FIRST_PATH, SECOND_PATH)
        }

        try {
            withTimeout(5.seconds) {
                firstStarted.await()
                secondStarted.await()
            }
            release.countDown()
            withTimeout(5.seconds) {
                job.await()
                first.closed.await()
                second.closed.await()
            }
        } finally {
            release.countDown()
            job.cancelAndJoin()
        }
    }

    @Test
    fun `child resource failure is propagated`() = runSuspendIO {
        val classLoader = ResourceClassLoader(
            mapOf(
                FIRST_PATH to { "first\n".byteInputStream() },
            )
        )

        withContext(ContextClassLoader(classLoader)) {
            assertFailsWith<IllegalStateException> {
                DictionaryProvider.readWords(MISSING_PATH, FIRST_PATH)
            }
        }
    }

    @Test
    fun `child read failure cancels sibling and closes both resources`() = runSuspendIO {
        val failingStarted = CompletableDeferred<Unit>()
        val siblingStarted = CompletableDeferred<Unit>()
        val failureRelease = CountDownLatch(1)
        val failing = FailingInputStream(failingStarted, failureRelease)
        val sibling = BlockingInputStream(siblingStarted)
        val classLoader = ResourceClassLoader(
            mapOf(
                FAILING_PATH to { failing },
                SIBLING_PATH to { sibling },
            )
        )
        supervisorScope {
            val job = async(Dispatchers.Default + ContextClassLoader(classLoader)) {
                DictionaryProvider.readWords(FAILING_PATH, SIBLING_PATH)
            }

            try {
                withTimeout(5.seconds) {
                    failingStarted.await()
                    siblingStarted.await()
                }
                failureRelease.countDown()
                assertFailsWith<IOException> { job.await() }
                withTimeout(5.seconds) {
                    failing.closed.await()
                    sibling.closed.await()
                }
            } finally {
                failureRelease.countDown()
                sibling.release()
                job.cancelAndJoin()
            }
        }
    }

    @Test
    fun `실제 Job 취소가 blocking child와 resource cleanup으로 전파된다`() = runSuspendIO {
        val started = CompletableDeferred<Unit>()
        val stream = BlockingInputStream(started)
        val classLoader = ResourceClassLoader(mapOf(SLOW_PATH to { stream }))
        val job = async(Dispatchers.Default + ContextClassLoader(classLoader)) {
            DictionaryProvider.readWords(SLOW_PATH)
        }

        try {
            withTimeout(5.seconds) { started.await() }

            job.cancel()
            withTimeout(5.seconds) {
                stream.closed.await()
                job.join()
            }
            job.isCancelled.shouldBeTrue()
        } finally {
            stream.release()
            job.cancelAndJoin()
        }
    }

    @Test
    fun `실제 Job 취소가 readWordsAsSet blocking child와 resource cleanup으로 전파된다`() = runSuspendIO {
        val started = CompletableDeferred<Unit>()
        val stream = BlockingInputStream(started)
        val classLoader = ResourceClassLoader(mapOf(SLOW_PATH to { stream }))
        val job = async(Dispatchers.Default + ContextClassLoader(classLoader)) {
            DictionaryProvider.readWordsAsSet(SLOW_PATH)
        }

        try {
            withTimeout(5.seconds) { started.await() }

            job.cancel()
            withTimeout(5.seconds) {
                stream.closed.await()
                job.join()
            }
            job.isCancelled.shouldBeTrue()
        } finally {
            stream.release()
            job.cancelAndJoin()
        }
    }

    private class ResourceClassLoader(
        private val resources: Map<String, () -> InputStream?>,
    ): ClassLoader(null) {
        override fun getResourceAsStream(name: String): InputStream? = resources[name]?.invoke()
    }

    private class TrackingInputStream(delegate: InputStream): FilterInputStream(delegate) {
        val closed = AtomicBoolean(false)

        override fun close() {
            closed.set(true)
            super.close()
        }
    }

    private class BarrierInputStream(
        private val started: CompletableDeferred<Unit>,
        private val release: CountDownLatch,
    ): InputStream() {
        val closed = CompletableDeferred<Unit>()

        override fun read(): Int {
            started.complete(Unit)
            return try {
                release.await()
                -1
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
                -1
            }
        }

        override fun close() {
            closed.complete(Unit)
            release.countDown()
        }
    }

    private class FailingInputStream(
        private val started: CompletableDeferred<Unit>,
        private val failureRelease: CountDownLatch,
    ): InputStream() {
        val closed = CompletableDeferred<Unit>()

        override fun read(): Int {
            started.complete(Unit)
            return try {
                failureRelease.await()
                throw IOException("synthetic dictionary read failure")
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
                -1
            }
        }

        override fun close() {
            closed.complete(Unit)
            failureRelease.countDown()
        }
    }

    private class BlockingInputStream(
        private val started: CompletableDeferred<Unit>,
    ): InputStream() {
        val closed = CompletableDeferred<Unit>()
        private val released = AtomicBoolean(false)

        override fun read(): Int {
            started.complete(Unit)
            while (!released.get()) {
                try {
                    Thread.sleep(10)
                } catch (_: InterruptedException) {
                    Thread.currentThread().interrupt()
                    return -1
                }
            }
            return -1
        }

        override fun close() {
            closed.complete(Unit)
            released.set(true)
        }

        fun release() {
            released.set(true)
        }
    }

    private class ContextClassLoader(
        private val classLoader: ClassLoader,
    ): ThreadContextElement<ClassLoader?>, AbstractCoroutineContextElement(Key) {
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
