package io.bluetape4k.tokenizer.korean.block

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.trace
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class PunctuationProcessorTest {

    companion object: KLogging()

    private val punctuationProcessor = PunctuationProcessor()

    @Test
    fun `중간 punctuation 제거하기`() = runTest {
        val actual = "중.@.고등#학교에서... 너는 뭐_했니? 난_1학년^^ 확.성기 섹.스 찌~~~찌~~~~~뽕"
        val punctuationRemoved = "중고등학교에서... 너는 뭐했니? 난1학년^^ 확성기 섹스 찌찌뽕"

        val removePunctuation = punctuationProcessor.removePunctuation(actual)
        log.trace { "remove punctuation=$removePunctuation" }
        removePunctuation shouldBeEqualTo punctuationRemoved
    }


    @Test
    fun `중간 punctuation이 여러개일 때`() = runTest {
        val actual = "중.@.고등#학교에서... 너는 뭐~.~했니? 난~ 1학년^^"
        val punctuationRemoved = "중고등학교에서... 너는 뭐했니? 난~ 1학년^^"

        val removePunctuation = punctuationProcessor.removePunctuation(actual)
        log.trace { "remove punctuation=$removePunctuation" }
        removePunctuation shouldBeEqualTo punctuationRemoved
    }

    @Test
    fun `중간 punctuation이 여러 개이고 공백이 있어도 제거하기`() = runTest {
        val actual = "중.@.고등#학교에서... 너는 뭐 ~ . ~ 했니? 난~ 1학년^^"
        val punctuationRemoved = "중고등학교에서... 너는 뭐했니? 난~ 1학년^^"

        val removePunctuation = punctuationProcessor.removePunctuation(actual)
        log.trace { "remove punctuation=$removePunctuation" }
        removePunctuation shouldBeEqualTo punctuationRemoved
    }

    @Test
    fun `URL은 보존하고 email은 기존 우회 규칙을 적용한다`() = runTest {
        val actual = "가https://example.com나와 가user@example.com나 " +
                "그리고 가 https://example.com . ~ 나와 가 user@example.com . ~ 나"
        val punctuationRemoved = "가https://example.com나와 가나 " +
                "그리고 가 https://example.com . ~ 나와 가나"

        val removePunctuation = punctuationProcessor.removePunctuation(actual)
        log.trace { "remove punctuation=$removePunctuation" }
        removePunctuation shouldBeEqualTo punctuationRemoved
    }

    @Test
    fun `정상 문장 구두점과 공백은 보존한다`() = runTest {
        val actual = "안녕? ! 잘가"

        val removePunctuation = punctuationProcessor.removePunctuation(actual)
        log.trace { "remove punctuation=$removePunctuation" }
        removePunctuation shouldBeEqualTo actual
    }

    @Test
    fun `findPunctuation은 실제 제거 계획을 반환한다`() = runTest {
        val actual = "뭐 ~ . ~ 했니"

        val removableTokens = punctuationProcessor.findPunctuation(actual)
            .filter { it.second }
            .map { it.first.text }

        removableTokens shouldBeEqualTo listOf(" ", "~", " ", ".", " ", "~", " ")
    }
}
