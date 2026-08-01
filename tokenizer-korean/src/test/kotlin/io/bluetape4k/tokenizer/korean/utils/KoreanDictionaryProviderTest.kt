package io.bluetape4k.tokenizer.korean.utils

import io.bluetape4k.logging.KLogging
import io.bluetape4k.tokenizer.korean.TestBase
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Noun
import io.bluetape4k.tokenizer.utils.CharArraySet
import io.bluetape4k.tokenizer.utils.DictionaryVersion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldNotBeEmpty
import org.junit.jupiter.api.Test

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
        nouns.contains(nonExistentWord).shouldBeTrue()
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
}
