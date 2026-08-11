package io.bluetape4k.tokenizer.japanese.utils

import io.bluetape4k.logging.KLogging
import io.bluetape4k.tokenizer.japanese.AbstractTokenizerTest
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldNotBeEmpty
import io.bluetape4k.tokenizer.utils.DictionaryVersion
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.ResourceLock

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
        blockwords.contains(newWord).shouldBeTrue()
        blockwords.contains(newWord2).shouldBeTrue()

        // 금칙어를 사전에서 제거합니다.
        JapaneseDictionaryProvider.removeBlockwords(listOf(newWord, newWord2))

        blockwords.contains(newWord).shouldBeFalse()
        blockwords.contains(newWord2).shouldBeFalse()
    }

    @Test
    fun `버전이 있는 금칙어 snapshot을 reload하고 원본을 복구한다`() {
        val original = JapaneseDictionaryProvider.currentBlockwordSnapshot()
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
                original.value,
            )
        }
    }

    @Test
    fun `반복 mutation은 revision과 visibility를 함께 갱신한다`() {
        val original = JapaneseDictionaryProvider.currentBlockwordSnapshot()
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
                original.value,
            )
        }
    }
}
