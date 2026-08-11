package io.bluetape4k.tokenizer.korean.utils

import io.bluetape4k.logging.KLogging
import io.bluetape4k.tokenizer.korean.TestBase
import io.bluetape4k.tokenizer.korean.KoreanProcessor
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Noun
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Verb
import io.bluetape4k.tokenizer.model.Severity
import io.bluetape4k.tokenizer.utils.CharArraySet
import io.bluetape4k.tokenizer.utils.DictionaryVersion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
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
import java.util.concurrent.atomic.AtomicLong

@ResourceLock("KoreanDictionaryProvider")
class KoreanDictionaryProviderTest: TestBase() {

    companion object: KLogging()

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
                Severity.LOW -> setOf(Severity.LOW, Severity.MIDDLE, Severity.HIGH)
                Severity.MIDDLE -> setOf(Severity.MIDDLE, Severity.HIGH)
                Severity.HIGH -> setOf(Severity.HIGH)
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
    fun `public dictionary view는 read-only이고 직접 변경을 snapshot에 기록하지 않는다`() {
        val directWord = "직접가변사전단어"
        val adverbs = KoreanDictionaryProvider.koreanDictionary.getValue(KoreanPos.Adverb)
        val highBlockwords = KoreanDictionaryProvider.blockWords.getValue(Severity.HIGH)

        assertFailsWith<UnsupportedOperationException> { adverbs.add(directWord) }
        assertFailsWith<UnsupportedOperationException> { highBlockwords.add(directWord) }
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
