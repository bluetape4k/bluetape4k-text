package io.bluetape4k.tokenizer.japanese

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.tokenizer.japanese.block.JapaneseBlockwordProcessor
import io.bluetape4k.tokenizer.japanese.tokenizer.JapaneseTokenizer
import io.bluetape4k.tokenizer.japanese.tokenizer.isNoun
import io.bluetape4k.tokenizer.model.MAX_BLOCKWORD_TEXT_LENGTH
import io.bluetape4k.tokenizer.model.MAX_TOKENIZE_TEXT_LENGTH
import io.bluetape4k.tokenizer.model.blockwordOptionsOf
import io.bluetape4k.tokenizer.model.blockwordRequestOf
import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEmpty
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldContain
import io.bluetape4k.assertions.shouldHaveSize
import io.bluetape4k.assertions.shouldNotBeEmpty
import io.bluetape4k.assertions.shouldNotContain
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.ResourceLock
import java.util.Locale

@ResourceLock("JapaneseDictionaryProvider")
class JapaneseProcessorTest: AbstractTokenizerTest() {

    companion object: KLogging()

    @Test
    fun `tokenize - 형태소 분석`() {
        val tokens = JapaneseProcessor.tokenize("お寿司が食べたい。")
        tokens.shouldNotBeEmpty()
        tokens.forEach { log.debug { "token=${it.surface}: ${it.allFeatures}" } }
    }

    @Test
    fun `tokenize - 빈 문자열`() {
        val tokens = JapaneseProcessor.tokenize("")
        tokens.shouldBeEmpty()
    }

    @Test
    fun `filterNoun - 명사 필터링`() {
        val tokens = JapaneseProcessor.tokenize("私は、日本語の勉強をしています。")
        val nouns = JapaneseProcessor.filterNoun(tokens).map { it.surface }

        nouns shouldHaveSize 3
        nouns shouldBeEqualTo listOf("私", "日本語", "勉強")
    }

    @Test
    fun `tokenize - mixed Korean Japanese text preserves Japanese surfaces`() {
        val cases = listOf(
            "오늘은 カフェで会議をした" to listOf("カフェ", "会議"),
            "서울から東京まで週末チケット" to listOf("東京", "週末", "チケット"),
            "ありがとう라고 말했더니 相手が笑った" to listOf("ありがとう", "相手"),
        )

        cases.forEach { (text, expectedSurfaces) ->
            val actual = JapaneseProcessor.tokenize(text).map { it.surface }

            expectedSurfaces.forEach { actual shouldContain it }
        }
    }

    @Test
    fun `filter - 커스텀 필터링`() {
        val tokens = JapaneseProcessor.tokenize("お寿司が食べたい。")
        val nouns = JapaneseProcessor.filter(tokens) { it.isNoun() }

        nouns shouldHaveSize 1
        nouns.first().surface shouldBeEqualTo "寿司"
    }

    @Test
    fun `findBlockwords - 금칙어 검색`() {
        val blockwords = JapaneseProcessor.findBlockwords("ホモの男性を理解できない")
        blockwords.shouldNotBeEmpty()
        blockwords.map { it.surface } shouldContain "ホモ"
    }

    @Test
    fun `findBlockwords - 금칙어 없음`() {
        val blockwords = JapaneseProcessor.findBlockwords("私は、日本語の勉強をしています。")
        blockwords.shouldBeEmpty()
    }

    @Test
    fun `maskBlockwords - 금칙어 마스킹`() {
        val request = blockwordRequestOf(
            "ホモの男性を理解できない",
            blockwordOptionsOf(locale = Locale.JAPANESE),
        )
        val response = JapaneseProcessor.maskBlockwords(request)

        response.blockwordExists.shouldBeTrue()
        log.debug { "maskedText=${response.maskedText}" }
    }

    @Test
    fun `blockword factory rejects oversized text before Japanese processor work`() {
        val rawText = "秘密の原文".repeat((MAX_BLOCKWORD_TEXT_LENGTH / 5) + 1)

        val exception = assertFailsWith<IllegalArgumentException> {
            blockwordRequestOf(rawText)
        }

        val message = exception.message.orEmpty()
        message shouldNotContain rawText
        message shouldNotContain "秘密の原文"
        message shouldContain MAX_BLOCKWORD_TEXT_LENGTH.toString()
    }

    @Test
    fun `tokenize facade rejects oversized text before Japanese tokenizer work`() {
        val rawText = "秘密の原文".repeat((MAX_TOKENIZE_TEXT_LENGTH / 5) + 1)

        assertOversizedTextRejected(rawText, MAX_TOKENIZE_TEXT_LENGTH) {
            JapaneseProcessor.tokenize(rawText)
        }
        assertOversizedTextRejected(rawText, MAX_TOKENIZE_TEXT_LENGTH) {
            JapaneseTokenizer.tokenize(rawText)
        }
    }

    @Test
    fun `blockword search rejects oversized text before Japanese tokenizer work`() {
        val rawText = "秘密の原文".repeat((MAX_BLOCKWORD_TEXT_LENGTH / 5) + 1)

        assertOversizedTextRejected(rawText, MAX_BLOCKWORD_TEXT_LENGTH) {
            JapaneseProcessor.findBlockwords(rawText)
        }
        assertOversizedTextRejected(rawText, MAX_BLOCKWORD_TEXT_LENGTH) {
            JapaneseBlockwordProcessor.findBlockwords(rawText)
        }
    }

    private fun assertOversizedTextRejected(
        rawText: String,
        maxLength: Int,
        block: () -> Unit,
    ) {
        val exception = assertFailsWith<IllegalArgumentException> {
            block()
        }

        val message = exception.message.orEmpty()
        message shouldNotContain rawText
        message shouldNotContain "秘密の原文"
        message shouldContain rawText.length.toString()
        message shouldContain maxLength.toString()
    }

    @Test
    fun `maskBlockwords - 금칙어 없는 경우`() {
        val request = blockwordRequestOf(
            "私は、日本語の勉強をしています。",
            blockwordOptionsOf(locale = Locale.JAPANESE),
        )
        val response = JapaneseProcessor.maskBlockwords(request)

        response.blockwordExists.shouldBeFalse()
    }

    @Test
    fun `addBlockwords and removeBlockwords - 사전 관리`() {
        // Kuromoji가 단일 명사로 인식하는 단어 사용
        val customWord = "東京"
        val dictionary = io.bluetape4k.tokenizer.japanese.utils.JapaneseDictionaryProvider.blockWordDictionary

        val wasBlockword = dictionary.contains(customWord)
        try {
            // 커스텀 금칙어 추가
            JapaneseProcessor.addBlockwords(listOf(customWord))
            io.bluetape4k.tokenizer.japanese.utils.JapaneseDictionaryProvider.blockWordDictionary
                .contains(customWord)
                .shouldBeTrue()

            val blockwords = JapaneseProcessor.findBlockwords("これは${customWord}です")
            blockwords.shouldNotBeEmpty()
            blockwords.map { it.surface } shouldContain customWord

            // 금칙어 제거
            JapaneseProcessor.removeBlockwords(listOf(customWord))
            io.bluetape4k.tokenizer.japanese.utils.JapaneseDictionaryProvider.blockWordDictionary
                .contains(customWord)
                .shouldBeFalse()
        } finally {
            if (!wasBlockword) {
                JapaneseProcessor.removeBlockwords(listOf(customWord))
            }
        }
    }
}
