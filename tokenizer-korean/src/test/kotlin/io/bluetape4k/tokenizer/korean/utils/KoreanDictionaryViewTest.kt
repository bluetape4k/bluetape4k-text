package io.bluetape4k.tokenizer.korean.utils

import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeSameInstanceAs
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.tokenizer.korean.KoreanProcessor
import io.bluetape4k.tokenizer.korean.TestBase
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Noun
import io.bluetape4k.tokenizer.model.Severity
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.ResourceLock

@ResourceLock("KoreanDictionaryProvider")
class KoreanDictionaryViewTest: TestBase() {

    @Test
    fun `같은 사전 값은 공개 뷰를 재사용하고 mutation 이후에도 이전 뷰는 변하지 않는다`() {
        val word = "issue334캐시검증명사"
        val beforeDictionary = KoreanDictionaryProvider.koreanDictionary
        val beforeBlockwords = KoreanDictionaryProvider.blockWords
        val beforeProperNouns = KoreanDictionaryProvider.properNouns
        KoreanDictionaryProvider.koreanDictionary shouldBeSameInstanceAs beforeDictionary
        KoreanDictionaryProvider.blockWords shouldBeSameInstanceAs beforeBlockwords
        KoreanDictionaryProvider.properNouns shouldBeSameInstanceAs beforeProperNouns

        try {
            KoreanProcessor.addBlockwords(listOf(word), Severity.HIGH)
            val addedDictionary = KoreanDictionaryProvider.koreanDictionary
            val addedBlockwords = KoreanDictionaryProvider.blockWords
            val addedProperNouns = KoreanDictionaryProvider.properNouns
            addedDictionary.getValue(Noun).contains(word).shouldBeTrue()
            addedBlockwords.getValue(Severity.HIGH).contains(word).shouldBeTrue()
            addedProperNouns.contains(word).shouldBeTrue()
            beforeDictionary.getValue(Noun).contains(word).shouldBeFalse()
            beforeBlockwords.getValue(Severity.HIGH).contains(word).shouldBeFalse()
            beforeProperNouns.contains(word).shouldBeFalse()
            KoreanDictionaryProvider.koreanDictionary shouldBeSameInstanceAs addedDictionary
            KoreanDictionaryProvider.blockWords shouldBeSameInstanceAs addedBlockwords
            KoreanDictionaryProvider.properNouns shouldBeSameInstanceAs addedProperNouns

            KoreanProcessor.removeBlockwords(listOf(word), Severity.HIGH)
            KoreanDictionaryProvider.koreanDictionary.getValue(Noun).contains(word).shouldBeFalse()
            KoreanDictionaryProvider.blockWords.getValue(Severity.HIGH).contains(word).shouldBeFalse()
            KoreanDictionaryProvider.properNouns.contains(word).shouldBeFalse()
            addedDictionary.getValue(Noun).contains(word).shouldBeTrue()
            addedBlockwords.getValue(Severity.HIGH).contains(word).shouldBeTrue()
            addedProperNouns.contains(word).shouldBeTrue()
        } finally {
            KoreanProcessor.removeBlockwords(listOf(word), Severity.HIGH)
        }
    }

}
