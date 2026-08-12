package io.bluetape4k.tokenizer.japanese.utils

import io.bluetape4k.logging.KLogging
import io.bluetape4k.tokenizer.japanese.AbstractTokenizerTest
import io.bluetape4k.tokenizer.model.Severity
import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeEmpty
import io.bluetape4k.assertions.shouldNotBeEmpty
import io.bluetape4k.assertions.shouldContainAll
import io.bluetape4k.junit5.concurrency.MultithreadingTester
import io.bluetape4k.tokenizer.utils.DictionaryVersion
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.ResourceLock
import java.util.Collections
import java.util.concurrent.atomic.AtomicLong

@ResourceLock("JapaneseDictionaryProvider")
class JapaneseDictionaryProviderTest: AbstractTokenizerTest() {

    companion object: KLogging()

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
