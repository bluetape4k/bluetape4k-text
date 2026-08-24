package io.bluetape4k.tokenizer.korean.utils

import io.bluetape4k.logging.KLogging
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.tokenizer.korean.TestBase
import io.bluetape4k.tokenizer.korean.KoreanProcessor
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Noun
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Verb
import io.bluetape4k.tokenizer.model.Severity
import io.bluetape4k.tokenizer.utils.CharArraySet
import io.bluetape4k.tokenizer.utils.DictionarySnapshot
import io.bluetape4k.tokenizer.utils.DictionaryVersion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.runBlocking
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeSameInstanceAs
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeEmpty
import io.bluetape4k.assertions.shouldNotBeEmpty
import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.junit5.concurrency.MultithreadingTester
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.ResourceLock
import java.util.Collections
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

@ResourceLock("KoreanDictionaryProvider")
class KoreanDictionaryProviderTest: TestBase() {

    companion object: KLogging()

    @Test
    fun `명시적 suspend preload API를 제공한다`() {
        KoreanDictionaryProvider::class.java.methods
            .any { method -> method.name == "preload" && method.parameterTypes.size == 1 }
            .shouldBeTrue()
    }

    @Test
    fun `preload은 주요 사전 snapshot을 준비한다`() = runSuspendIO {
        KoreanDictionaryProvider.preload()

        KoreanDictionaryProvider.allDictionariesInitialized().shouldBeTrue()
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
    fun `사전 로드하기`() {
        val nouns: CharArraySet = KoreanDictionaryProvider.koreanDictionary[Noun]!!
        nouns.shouldNotBeEmpty()
    }

    @Test
    fun `단어를 사전에 추가하기`() {
        val nonExistentWord = "없는명사다"

        val nouns = KoreanDictionaryProvider.koreanDictionary[Noun]!!

        nouns.contains(nonExistentWord).shouldBeFalse()
        nouns.contains("각광").shouldBeTrue()

        KoreanDictionaryProvider.addWordsToDictionary(Noun, listOf(nonExistentWord))
        KoreanDictionaryProvider.koreanDictionary.getValue(Noun).contains(nonExistentWord).shouldBeTrue()
    }

    @Test
    fun `load frequency`() {
        KoreanDictionaryProvider.koreanEntityFreq.shouldNotBeEmpty()
    }

    @Test
    fun `동시 추가에서도 사전 일관성을 유지한다`() = runBlocking {
        val words = (1..50).map { "동시추가명사_$it" }

        words.chunked(10)
            .map { chunk ->
                async(Dispatchers.Default) {
                    KoreanDictionaryProvider.addWordsToDictionary(Noun, chunk)
                }
            }
            .awaitAll()

        val nouns = KoreanDictionaryProvider.koreanDictionary[Noun]!!
        words.forEach { nouns.contains(it).shouldBeTrue() }
    }

    @Test
    fun `버전이 있는 사전 snapshot을 reload하고 원본을 복구한다`() {
        val original = KoreanDictionaryProvider.currentDictionarySnapshot()
        val updatedWord = "버전갱신명사"

        try {
            val updated = KoreanDictionaryProvider.reloadDictionaries(
                DictionaryVersion("korean-dictionary", original.version.revision + 1),
                mapOf(Noun to listOf(updatedWord)),
            )
            updated.value[Noun]!!.contains(updatedWord).shouldBeTrue()
            KoreanDictionaryProvider.currentDictionarySnapshot().version shouldBeEqualTo updated.version
        } finally {
            KoreanDictionaryProvider.reloadDictionaries(
                DictionaryVersion("korean-dictionary", original.version.revision + 2),
                original.value,
            )
        }
    }

    @Test
    fun `품사 mutation은 변경 entry만 새로 복사한다`() {
        val original = KoreanDictionaryProvider.currentDictionarySnapshot()
        val addedWord = "COW품사단어"

        try {
            val before = KoreanDictionaryProvider.currentDictionarySnapshot()
            KoreanDictionaryProvider.addWordsToDictionary(Noun, listOf(addedWord))
            val after = KoreanDictionaryProvider.currentDictionarySnapshot()

            (before.value[Noun] !== after.value[Noun]).shouldBeTrue()
            before.value[Verb] shouldBeSameInstanceAs after.value[Verb]
            after.value[Noun]!!.contains(addedWord).shouldBeTrue()
        } finally {
            KoreanDictionaryProvider.reloadDictionaries(
                DictionaryVersion(
                    "korean-dictionary",
                    KoreanDictionaryProvider.currentDictionarySnapshot().version.revision + 1,
                ),
                original.value,
            )
        }
    }

    @Test
    fun `semantic no-op은 revision만 증가시키고 snapshot map과 entry를 재사용한다`() {
        val before = KoreanDictionaryProvider.currentDictionarySnapshot()

        KoreanDictionaryProvider.addWordsToDictionary(Noun, listOf("각광"))

        val after = KoreanDictionaryProvider.currentDictionarySnapshot()
        before.version.revision.inc() shouldBeEqualTo after.version.revision
        before.value shouldBeSameInstanceAs after.value
        before.value[Noun] shouldBeSameInstanceAs after.value[Noun]
    }

    @Test
    fun `severity fan-out은 실제 변경 entry만 복사한다`() {
        Severity.values().forEach { severity ->
            val original = KoreanDictionaryProvider.currentBlockwordSnapshot()
            val addedWord = "COW금칙어_${severity.name}"
            val affected = when (severity) {
                Severity.LOW -> setOf(Severity.LOW)
                Severity.MIDDLE -> setOf(Severity.LOW, Severity.MIDDLE)
                Severity.HIGH -> setOf(Severity.LOW, Severity.MIDDLE, Severity.HIGH)
            }

            try {
                KoreanDictionaryProvider.mutateBlockwords(severity) {
                    add(addedWord)
                }
                val after = KoreanDictionaryProvider.currentBlockwordSnapshot()

                Severity.values().forEach { candidate ->
                    if (candidate in affected) {
                        (original.value[candidate] !== after.value[candidate]).shouldBeTrue()
                        after.value[candidate]!!.contains(addedWord).shouldBeTrue()
                    } else {
                        original.value[candidate] shouldBeSameInstanceAs after.value[candidate]
                    }
                }
            } finally {
                KoreanDictionaryProvider.reloadBlockwords(
                    DictionaryVersion(
                        "korean-blockwords",
                        KoreanDictionaryProvider.currentBlockwordSnapshot().version.revision + 1,
                    ),
                    original.value,
                )
            }
        }
    }

    @Test
    fun `runtime severity mutation은 source tier를 threshold cumulative view에 올바르게 반영한다`() {
        val original = KoreanDictionaryProvider.currentBlockwordSnapshot()
        val words = Severity.values().associateWith { severity ->
            "issue241-runtime-${severity.name.lowercase()}"
        }
        val expected = mapOf(
            Severity.LOW to words.values.toSet(),
            Severity.MIDDLE to setOf(words.getValue(Severity.MIDDLE), words.getValue(Severity.HIGH)),
            Severity.HIGH to setOf(words.getValue(Severity.HIGH)),
        )

        try {
            words.forEach { (severity, word) ->
                KoreanDictionaryProvider.mutateBlockwords(severity) {
                    add(word)
                }
            }

            val actual = KoreanDictionaryProvider.currentBlockwordSnapshot().value
            assertBlockwordEntries(actual, words.values, expected)
            words.forEach { (source, word) ->
                Severity.values().forEach { threshold ->
                    val contains = KoreanDictionaryProvider.containsBlockword(word, threshold)
                    if (threshold.ordinal <= source.ordinal) contains.shouldBeTrue() else contains.shouldBeFalse()
                }
            }
        } finally {
            KoreanDictionaryProvider.reloadBlockwords(
                DictionaryVersion(
                    "korean-blockwords",
                    KoreanDictionaryProvider.currentBlockwordSnapshot().version.revision + 1,
                ),
                original.value,
            )
        }
    }

    @Test
    fun `reload은 exact tier를 threshold view로 정규화한다`() {
        val original = KoreanDictionaryProvider.currentBlockwordSnapshot()
        val words = mapOf(
            Severity.LOW to "issue241-reload-low",
            Severity.MIDDLE to "issue241-reload-middle",
            Severity.HIGH to "issue241-reload-high",
        )
        val exactTierInput = words.mapValues { (_, word) -> listOf(word) }

        try {
            KoreanDictionaryProvider.reloadBlockwords(
                DictionaryVersion(
                    "korean-blockwords",
                    original.version.revision + 1,
                ),
                exactTierInput,
            )

            val reloaded = KoreanDictionaryProvider.currentBlockwordSnapshot().value
            assertBlockwordEntries(
                reloaded,
                words.values,
                mapOf(
                    Severity.LOW to words.values.toSet(),
                    Severity.MIDDLE to setOf(
                        words.getValue(Severity.MIDDLE),
                        words.getValue(Severity.HIGH),
                    ),
                    Severity.HIGH to setOf(words.getValue(Severity.HIGH)),
                ),
            )
        } finally {
            restoreBlockwords(original)
        }
    }

    @Test
    fun `remove와 clear도 severity tier와 threshold view를 보존한다`() {
        val original = KoreanDictionaryProvider.currentBlockwordSnapshot()
        val words = mapOf(
            Severity.LOW to "issue241-remove-low",
            Severity.MIDDLE to "issue241-remove-middle",
            Severity.HIGH to "issue241-remove-high",
        )

        try {
            KoreanDictionaryProvider.reloadBlockwords(
                DictionaryVersion("korean-blockwords", original.version.revision + 1),
                words.mapValues { (_, word) -> listOf(word) },
            )
            KoreanDictionaryProvider.mutateBlockwords(Severity.MIDDLE) {
                remove(words.getValue(Severity.MIDDLE))
            }
            var after = KoreanDictionaryProvider.currentBlockwordSnapshot().value
            assertBlockwordEntries(
                after,
                words.values,
                mapOf(
                    Severity.LOW to setOf(words.getValue(Severity.LOW), words.getValue(Severity.HIGH)),
                    Severity.MIDDLE to setOf(words.getValue(Severity.HIGH)),
                    Severity.HIGH to setOf(words.getValue(Severity.HIGH)),
                ),
            )

            KoreanDictionaryProvider.mutateBlockwords(Severity.LOW) {
                val changed = isNotEmpty()
                clear()
                changed
            }
            after = KoreanDictionaryProvider.currentBlockwordSnapshot().value
            assertBlockwordEntries(
                after,
                words.values,
                Severity.values().associateWith { setOf(words.getValue(Severity.HIGH)) },
            )
        } finally {
            restoreBlockwords(original)
        }
    }

    private fun restoreBlockwords(
        original: DictionarySnapshot<Map<Severity, Set<String>>>,
    ) {
        KoreanDictionaryProvider.reloadBlockwords(
            DictionaryVersion(
                "korean-blockwords",
                KoreanDictionaryProvider.currentBlockwordSnapshot().version.revision + 1,
            ),
            original.value,
        )
    }

    private fun assertBlockwordEntries(
        actual: Map<Severity, Set<String>>,
        trackedWords: Collection<String>,
        expected: Map<Severity, Set<String>>,
    ) {
        val tracked = trackedWords.toSet()
        Severity.values().forEach { severity ->
            actual.getValue(severity).filter { it in tracked }.toSet() shouldBeEqualTo expected.getValue(severity)
        }
    }

    @Test
    fun `blockword semantic no-op은 snapshot map과 entry를 재사용한다`() {
        val before = KoreanDictionaryProvider.currentBlockwordSnapshot()

        KoreanDictionaryProvider.mutateBlockwords(Severity.HIGH) {
            remove("__missing_issue239_blockword__")
        }

        val after = KoreanDictionaryProvider.currentBlockwordSnapshot()
        before.version.revision.inc() shouldBeEqualTo after.version.revision
        before.value shouldBeSameInstanceAs after.value
        before.value[Severity.HIGH] shouldBeSameInstanceAs after.value[Severity.HIGH]
    }

    @Test
    fun `facade의 noun 제거도 versioned snapshot을 갱신한다`() {
        val original = KoreanDictionaryProvider.currentDictionarySnapshot()
        val originalBlockwords = KoreanDictionaryProvider.currentBlockwordSnapshot()
        val addedWord = "facade제거금칙어"

        try {
            KoreanProcessor.addBlockwords(listOf(addedWord), Severity.HIGH)
            val added = KoreanDictionaryProvider.currentDictionarySnapshot()

            KoreanProcessor.removeBlockwords(listOf(addedWord), Severity.HIGH)
            val removed = KoreanDictionaryProvider.currentDictionarySnapshot()

            (removed.version.revision > added.version.revision).shouldBeTrue()
            removed.value[Noun]!!.contains(addedWord).shouldBeFalse()
        } finally {
            KoreanDictionaryProvider.reloadDictionaries(
                DictionaryVersion(
                    "korean-dictionary",
                    KoreanDictionaryProvider.currentDictionarySnapshot().version.revision + 1,
                ),
                original.value,
            )
            KoreanDictionaryProvider.reloadBlockwords(
                DictionaryVersion(
                    "korean-blockwords",
                    KoreanDictionaryProvider.currentBlockwordSnapshot().version.revision + 1,
                ),
                originalBlockwords.value,
            )
        }
    }

    @Test
    @Suppress("DEPRECATION")
    fun `금칙어 facade mutation은 세 사전 snapshot을 함께 갱신하고 deprecated alias도 대칭이다`() {
        val word = "P1원자금칙어_295"

        try {
            KoreanProcessor.addBlockwords(listOf(word), Severity.HIGH)
            val added = KoreanDictionaryProvider.currentBlockwordBundleSnapshot()

            added.blockwords.version.revision shouldBeEqualTo added.dictionary.version.revision
            added.dictionary.version.revision shouldBeEqualTo added.properNouns.version.revision
            added.blockwords.value.getValue(Severity.HIGH).contains(word).shouldBeTrue()
            added.dictionary.value.getValue(Noun).contains(word).shouldBeTrue()
            added.properNouns.value.contains(word).shouldBeTrue()

            KoreanProcessor.removeBlockword(listOf(word), Severity.HIGH)
            val removed = KoreanDictionaryProvider.currentBlockwordBundleSnapshot()
            removed.blockwords.value.getValue(Severity.HIGH).contains(word).shouldBeFalse()
            removed.dictionary.value.getValue(Noun).contains(word).shouldBeFalse()
            removed.properNouns.value.contains(word).shouldBeFalse()
        } finally {
            KoreanProcessor.removeBlockwords(listOf(word), Severity.HIGH)
        }
    }

    @Test
    fun `동시 add remove 중 aggregate snapshot은 mixed revision을 노출하지 않는다`() {
        val word = "P1동시원자금칙어_295"
        val violations = Collections.synchronizedList(mutableListOf<String>())

        fun observe() {
            val snapshot = KoreanDictionaryProvider.currentBlockwordBundleSnapshot()
            val blockword = snapshot.blockwords.value.getValue(Severity.HIGH).contains(word)
            val noun = snapshot.dictionary.value.getValue(Noun).contains(word)
            val properNoun = snapshot.properNouns.value.contains(word)
            if (setOf(blockword, noun, properNoun).distinct().size != 1) {
                violations.add("mixed aggregate snapshot: blockword=$blockword noun=$noun properNoun=$properNoun")
            }
            if (snapshot.blockwords.version.revision != snapshot.dictionary.version.revision ||
                snapshot.dictionary.version.revision != snapshot.properNouns.version.revision
            ) {
                violations.add(
                    "mixed aggregate revision: blockword=${snapshot.blockwords.version.revision} " +
                            "dictionary=${snapshot.dictionary.version.revision} " +
                            "properNouns=${snapshot.properNouns.version.revision}"
                )
            }
        }

        try {
            // 이전 테스트의 독립 reload가 있더라도 aggregate mutation이 revision을 재정렬하도록 준비합니다.
            KoreanProcessor.addBlockwords(listOf(word), Severity.HIGH)
            KoreanProcessor.removeBlockwords(listOf(word), Severity.HIGH)

            MultithreadingTester()
                .workers(8)
                .rounds(100)
                .add {
                    KoreanProcessor.addBlockwords(listOf(word), Severity.HIGH)
                    observe()
                }
                .add {
                    KoreanProcessor.removeBlockwords(listOf(word), Severity.HIGH)
                    observe()
                }
                .add { observe() }
                .run()
        } finally {
            KoreanProcessor.removeBlockwords(listOf(word), Severity.HIGH)
        }

        violations.shouldBeEmpty()
    }

    @Test
    fun `public dictionary view는 read-only이고 직접 변경을 snapshot에 기록하지 않는다`() {
        val directWord = "직접가변사전단어"
        val adverbs = KoreanDictionaryProvider.koreanDictionary.getValue(KoreanPos.Adverb)
        val highBlockwords = KoreanDictionaryProvider.blockWords.getValue(Severity.HIGH)
        val properNouns = KoreanDictionaryProvider.properNouns

        assertFailsWith<UnsupportedOperationException> { adverbs.add(directWord) }
        assertFailsWith<UnsupportedOperationException> { highBlockwords.add(directWord) }
        assertFailsWith<UnsupportedOperationException> { properNouns.add(directWord) }
        KoreanDictionaryProvider.currentDictionarySnapshot()
            .value[KoreanPos.Adverb]
            ?.contains(directWord)
            .shouldBeFalse()
    }

    @Test
    fun `동시 reload 중 public dictionary view는 완전한 snapshot만 노출한다`() {
        val original = KoreanDictionaryProvider.currentDictionarySnapshot()
        val stateA = KoreanPos.values().associateWith { pos ->
            (0 until 128).map { index -> "atomic-a-${pos.name}-$index" }
        }
        val stateB = KoreanPos.values().associateWith { pos ->
            (0 until 128).map { index -> "atomic-b-${pos.name}-$index" }
        }
        val expectedStates = setOf(
            stateA.mapValues { (_, words) -> words.toSet() },
            stateB.mapValues { (_, words) -> words.toSet() },
        )
        val violations = Collections.synchronizedList(mutableListOf<String>())
        val revision = AtomicLong(original.version.revision)

        try {
            KoreanDictionaryProvider.reloadDictionaries(
                DictionaryVersion("korean-dictionary", revision.incrementAndGet()),
                stateA,
            )

            MultithreadingTester()
                .workers(8)
                .rounds(500)
                .add {
                    synchronized(revision) {
                        val nextRevision = revision.incrementAndGet()
                        KoreanDictionaryProvider.reloadDictionaries(
                            DictionaryVersion("korean-dictionary", nextRevision),
                            if (nextRevision % 2L == 0L) stateA else stateB,
                        )
                    }
                }
                .add {
                    try {
                        val observed = KoreanDictionaryProvider.koreanDictionary
                            .mapValues { (_, words) ->
                                words.map { word ->
                                    when (word) {
                                        is CharArray -> word.concatToString()
                                        else -> word.toString()
                                    }
                                }.toSet()
                            }
                        if (observed !in expectedStates) {
                            violations.add("partial dictionary state observed: entries=${observed.size}")
                        }
                    } catch (e: RuntimeException) {
                        violations.add("dictionary read failed: ${e::class.simpleName}")
                    }
                }
                .run()
        } finally {
            KoreanDictionaryProvider.reloadDictionaries(
                DictionaryVersion("korean-dictionary", revision.incrementAndGet()),
                original.value,
            )
        }

        violations.shouldBeEmpty()
    }
}
