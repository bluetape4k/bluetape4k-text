package io.bluetape4k.tokenizer.japanese.utils

import io.bluetape4k.logging.KLogging
import io.bluetape4k.tokenizer.japanese.AbstractTokenizerTest
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldNotBeEmpty
import org.junit.jupiter.api.Test

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
}
