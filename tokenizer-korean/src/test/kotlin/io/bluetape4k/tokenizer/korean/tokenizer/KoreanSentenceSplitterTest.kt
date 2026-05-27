package io.bluetape4k.tokenizer.korean.tokenizer

import io.bluetape4k.logging.KLogging
import io.bluetape4k.tokenizer.korean.TestBase
import io.bluetape4k.tokenizer.korean.tokenizer.KoreanSentenceSplitter.split
import io.bluetape4k.tokenizer.model.MAX_TOKENIZE_TEXT_LENGTH
import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldContain
import io.bluetape4k.assertions.shouldContainSame
import io.bluetape4k.assertions.shouldNotContain
import org.junit.jupiter.api.Test


class KoreanSentenceSplitterTest: TestBase() {

    companion object: KLogging()

    @Test
    fun `should split a string into sentences`() {
        var actual = split("안녕? iphone6안녕? 세상아?").toList()
        actual shouldContainSame listOf(
            Sentence("안녕?", 0, 3),
            Sentence("iphone6안녕?", 4, 14),
            Sentence("세상아?", 15, 19)
        )

        actual = split("그런데, 누가 그러는데, 루루가 있대. 그렇대? 그렇지! 아리고 이럴수가!!!!! 그래...").toList()
        actual shouldContainSame listOf(
            Sentence("그런데, 누가 그러는데, 루루가 있대.", 0, 21),
            Sentence("그렇대?", 22, 26),
            Sentence("그렇지!", 27, 31),
            Sentence("아리고 이럴수가!!!!!", 32, 45),
            Sentence("그래...", 46, 51)
        )

        actual = split("이게 말이 돼?! 으하하하 ㅋㅋㅋㅋㅋㅋㅋ…    ").toList()
        actual shouldContainSame listOf(
            Sentence("이게 말이 돼?!", 0, 9),
            Sentence("으하하하 ㅋㅋㅋㅋㅋㅋㅋ…", 10, 23)
        )
    }

    @Test
    fun `split rejects oversized text without leaking raw input`() {
        val rawText = "민감한원문".repeat((MAX_TOKENIZE_TEXT_LENGTH / 5) + 1)

        val exception = assertFailsWith<IllegalArgumentException> {
            split(rawText).toList()
        }

        val message = exception.message.orEmpty()
        message shouldNotContain rawText
        message shouldNotContain "민감한원문"
        message shouldContain rawText.length.toString()
        message shouldContain MAX_TOKENIZE_TEXT_LENGTH.toString()
    }
}
