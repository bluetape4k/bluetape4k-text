package io.bluetape4k.tokenizer.japanese.utils

import io.bluetape4k.logging.KLogging
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.tokenizer.japanese.AbstractTokenizerTest
import io.bluetape4k.tokenizer.model.Severity
import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeSameInstanceAs
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeEmpty
import io.bluetape4k.assertions.shouldNotBeEmpty
import io.bluetape4k.assertions.shouldContainAll
import io.bluetape4k.junit5.concurrency.MultithreadingTester
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.awaitCancellation
import io.bluetape4k.tokenizer.utils.DictionaryVersion
import io.bluetape4k.tokenizer.utils.SuspendMemoized
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.ResourceLock
import java.util.Collections
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

@ResourceLock("JapaneseDictionaryProvider")
class JapaneseDictionaryProviderTest: AbstractTokenizerTest() {

    companion object: KLogging()

    @Test
    fun `공개 금칙어 뷰는 재사용되며 add remove clear reload 이후 갱신된다`() {
        val original = JapaneseDictionaryProvider.currentBlockwordSeveritySnapshot()
        val word = "issue334追加単語"
        val before = JapaneseDictionaryProvider.blockWordDictionary
        JapaneseDictionaryProvider.blockWordDictionary shouldBeSameInstanceAs before
        try {
            JapaneseDictionaryProvider.addBlockwords(listOf(word))
            val added = JapaneseDictionaryProvider.blockWordDictionary
            added.contains(word).shouldBeTrue()
            before.contains(word).shouldBeFalse()
            JapaneseDictionaryProvider.blockWordDictionary shouldBeSameInstanceAs added
            JapaneseDictionaryProvider.removeBlockwords(listOf(word))
            JapaneseDictionaryProvider.blockWordDictionary.contains(word).shouldBeFalse()
            added.contains(word).shouldBeTrue()
            JapaneseDictionaryProvider.clearBlockwords()
            JapaneseDictionaryProvider.blockWordDictionary.shouldBeEmpty()
            added.contains(word).shouldBeTrue()
        } finally {
            JapaneseDictionaryProvider.reloadBlockwords(
                DictionaryVersion(
                    "japanese-blockwords",
                    JapaneseDictionaryProvider.currentBlockwordSnapshot().version.revision + 1,
                ),
                original.value,
            )
        }
        JapaneseDictionaryProvider.blockWordDictionary.size shouldBeEqualTo before.size
        JapaneseDictionaryProvider.blockWordDictionary.contains(word).shouldBeFalse()
    }

    @Test
    fun `명시적 suspend preload API를 제공한다`() {
        JapaneseDictionaryProvider::class.java.methods
            .any { method -> method.name == "preload" && method.parameterTypes.size == 1 }
            .shouldBeTrue()
    }

    @Test
    fun `preload은 금칙어 snapshot을 준비한다`() = runSuspendIO {
        JapaneseDictionaryProvider.preload()
        JapaneseDictionaryProvider.allDictionariesInitialized().shouldBeTrue()
    }

    @Test
    fun `동시 suspend 초기화는 loader를 한 번만 실행하고 취소 후 재시도한다`() = runSuspendIO {
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
    fun `금칙어 사전 로드하기`() {
        val blockwords = JapaneseDictionaryProvider.blockWordDictionary
        blockwords.shouldNotBeEmpty()
    }

    @Test
    fun `금칙어 사전에 등록된 단어 검증`() {
        val blockwords = JapaneseDictionaryProvider.blockWordDictionary
        blockwords.contains("한국어").shouldBeFalse()
        blockwords.contains("性器").shouldBeTrue()
    }

    @Test
    fun `금칙어 동적 추가 삭제`() {
        val blockwords = JapaneseDictionaryProvider.blockWordDictionary

        val newWord = "19禁"
        val newWord2 = "29禁"

        // 금칙어를 사전에 추가합니다.
        blockwords.contains(newWord).shouldBeFalse()
        blockwords.contains(newWord2).shouldBeFalse()
        JapaneseDictionaryProvider.addBlockwords(listOf(newWord, newWord2))
        JapaneseDictionaryProvider.blockWordDictionary.contains(newWord).shouldBeTrue()
        JapaneseDictionaryProvider.blockWordDictionary.contains(newWord2).shouldBeTrue()

        // 금칙어를 사전에서 제거합니다.
        JapaneseDictionaryProvider.removeBlockwords(listOf(newWord, newWord2))

        JapaneseDictionaryProvider.blockWordDictionary.contains(newWord).shouldBeFalse()
        JapaneseDictionaryProvider.blockWordDictionary.contains(newWord2).shouldBeFalse()
    }

    @Test
    fun `severity snapshot은 cumulative threshold로 정규화된다`() {
        val original = JapaneseDictionaryProvider.currentBlockwordSeveritySnapshot()
        val replacement = mapOf(
            Severity.LOW to listOf("jp-low"),
            Severity.MIDDLE to listOf("jp-middle"),
            Severity.HIGH to listOf("jp-high"),
        )

        try {
            JapaneseDictionaryProvider.reloadBlockwords(
                DictionaryVersion("japanese-blockwords", original.version.revision + 1),
                replacement,
            )
            val value = JapaneseDictionaryProvider.currentBlockwordSeveritySnapshot().value

            value.getValue(Severity.LOW) shouldContainAll
                    listOf("jp-low", "jp-middle", "jp-high")
            value.getValue(Severity.MIDDLE) shouldContainAll
                    listOf("jp-middle", "jp-high")
            value.getValue(Severity.HIGH) shouldContainAll
                    listOf("jp-high")
        } finally {
            JapaneseDictionaryProvider.reloadBlockwords(
                DictionaryVersion(
                    "japanese-blockwords",
                    JapaneseDictionaryProvider.currentBlockwordSnapshot().version.revision + 1,
                ),
                original.value,
            )
        }
    }

    @Test
    fun `버전이 있는 금칙어 snapshot을 reload하고 원본을 복구한다`() {
        val original = JapaneseDictionaryProvider.currentBlockwordSnapshot()
        val originalBySeverity = JapaneseDictionaryProvider.currentBlockwordSeveritySnapshot()
        val updatedWord = "版관리"

        try {
            val updated = JapaneseDictionaryProvider.reloadBlockwords(
                DictionaryVersion("japanese-blockwords", original.version.revision + 1),
                listOf(updatedWord),
            )
            JapaneseDictionaryProvider.containsBlockword(updatedWord).shouldBeTrue()
            updated.value shouldBeEqualTo setOf(updatedWord)
        } finally {
            JapaneseDictionaryProvider.reloadBlockwords(
                DictionaryVersion("japanese-blockwords", original.version.revision + 2),
                originalBySeverity.value,
            )
        }
    }

    @Test
    fun `반복 mutation은 revision과 visibility를 함께 갱신한다`() {
        val original = JapaneseDictionaryProvider.currentBlockwordSnapshot()
        val originalBySeverity = JapaneseDictionaryProvider.currentBlockwordSeveritySnapshot()
        val newWord = "版관리반복"

        try {
            repeat(3) { index ->
                val before = JapaneseDictionaryProvider.currentBlockwordSnapshot()
                JapaneseDictionaryProvider.addBlockwords(listOf(newWord))
                val added = JapaneseDictionaryProvider.currentBlockwordSnapshot()

                (added.version.revision > before.version.revision).shouldBeTrue()
                JapaneseDictionaryProvider.containsBlockword(newWord).shouldBeTrue()

                JapaneseDictionaryProvider.removeBlockwords(listOf(newWord))
                val removed = JapaneseDictionaryProvider.currentBlockwordSnapshot()
                (removed.version.revision > added.version.revision).shouldBeTrue()
                JapaneseDictionaryProvider.containsBlockword(newWord).shouldBeFalse()
                removed.value.contains(newWord).shouldBeFalse()

                if (index == 2) {
                    removed.version.revision shouldBeEqualTo before.version.revision + 2
                }
            }
        } finally {
            JapaneseDictionaryProvider.reloadBlockwords(
                DictionaryVersion(
                    "japanese-blockwords",
                    JapaneseDictionaryProvider.currentBlockwordSnapshot().version.revision + 1,
                ),
                originalBySeverity.value,
            )
        }
    }

    @Test
    fun `public blockword view는 read-only다`() {
        val blockwords = JapaneseDictionaryProvider.blockWordDictionary

        assertFailsWith<UnsupportedOperationException> {
            blockwords.add("직접변경은허용하지않음")
        }
    }

    @Test
    fun `동시 reload 중 public blockword view는 완전한 snapshot만 노출한다`() {
        val original = JapaneseDictionaryProvider.currentBlockwordSnapshot()
        val originalBySeverity = JapaneseDictionaryProvider.currentBlockwordSeveritySnapshot()
        val stateA = listOf("jp-atomic-a-1", "jp-atomic-a-2")
        val stateB = listOf("jp-atomic-b-1", "jp-atomic-b-2")
        val expectedStates = setOf(stateA.toSet(), stateB.toSet())
        val violations = Collections.synchronizedList(mutableListOf<String>())
        val revision = AtomicLong(original.version.revision)

        try {
            JapaneseDictionaryProvider.reloadBlockwords(
                DictionaryVersion("japanese-blockwords", revision.incrementAndGet()),
                stateA,
            )

            MultithreadingTester()
                .workers(8)
                .rounds(500)
                .add {
                    synchronized(revision) {
                        val nextRevision = revision.incrementAndGet()
                        JapaneseDictionaryProvider.reloadBlockwords(
                            DictionaryVersion("japanese-blockwords", nextRevision),
                            if (nextRevision % 2L == 0L) stateA else stateB,
                        )
                    }
                }
                .add {
                    try {
                        val observed = JapaneseDictionaryProvider.blockWordDictionary
                            .map { word ->
                                when (word) {
                                    is CharArray -> word.concatToString()
                                    else -> word.toString()
                                }
                            }.toSet()
                        if (observed !in expectedStates) {
                            violations.add("partial blockword state observed: size=${observed.size}")
                        }
                    } catch (e: RuntimeException) {
                        violations.add("blockword read failed: ${e::class.simpleName}")
                    }
                }
                .run()
        } finally {
            JapaneseDictionaryProvider.reloadBlockwords(
                DictionaryVersion("japanese-blockwords", revision.incrementAndGet()),
                originalBySeverity.value,
            )
        }

        violations.shouldBeEmpty()
    }
}
