package io.bluetape4k.text.search

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldContain
import io.bluetape4k.assertions.shouldHaveSize
import io.bluetape4k.assertions.shouldNotContain
import org.junit.jupiter.api.Test

class RedactionTest {

    @Test
    fun `keyword and regex matches merge overlaps and preserve adjacent spans`() {
        val policy = RedactionPolicy.of(
            rules = listOf(
                RedactionRule.keyword("keyword.account", "keyword", "account", priority = 30),
                RedactionRule.keyword("keyword.account-number", "keyword", "account number", priority = 10),
                RedactionRule.regex("regex.review", "review", "review", priority = 20),
            )
        )

        val result = TextRedactor.of(policy).redact("account number review")
        val expected = "*".repeat("account number".length) + " " + "*".repeat("review".length)

        result.redactedText shouldBeEqualTo expected
        result.spans shouldHaveSize 2
        result.spans[0].range shouldBeEqualTo RedactionRange.of(0, "account number".length)
        result.spans[0].category shouldBeEqualTo "keyword"
        result.spans[0].ruleIds shouldBeEqualTo listOf("keyword.account-number", "keyword.account")
        result.spans[1].range shouldBeEqualTo RedactionRange.of(
            "account number ".length,
            "account number review".length,
        )
    }

    @Test
    fun `decomposed Unicode and surrogate pair offsets refer to original UTF-16 text`() {
        val composed = "café"
        val decomposed = "cafe\u0301"
        val emoji = "\uD83D\uDE00"
        val input = "$emoji $decomposed"
        val policy = RedactionPolicy.of(
            rules = listOf(RedactionRule.keyword("keyword.cafe", "keyword", composed)),
            keywordNormalization = NormalizationForm.NFC,
        )

        val result = TextRedactor.of(policy).redact(input)

        result.redactedText.length shouldBeEqualTo input.length
        result.spans.single().range shouldBeEqualTo RedactionRange.of(emoji.length + 1, input.length)
        result.redactedText.substring(emoji.length + 1).all { it == '*' }.shouldBeTrue()
    }

    @Test
    fun `empty input is stable and over-limit input is rejected without echo`() {
        val redactor = TextRedactor.of(
            RedactionPolicy.of(
                rules = listOf(RedactionRule.keyword("keyword.safe", "keyword", "secret")),
                maxTextLength = 3,
            )
        )

        val empty = redactor.redact("")
        empty.redactedText shouldBeEqualTo ""
        empty.spans shouldHaveSize 0

        val raw = "secret"
        val failure = assertFailsWith<IllegalArgumentException> { redactor.redact(raw) }
        (failure.message ?: "") shouldNotContain raw
    }

    @Test
    fun `policy snapshots and metadata never expose raw rule values`() {
        val keyword = "account number"
        val pattern = "secret-[0-9]+"
        val rules = mutableListOf(
            RedactionRule.keyword("keyword.account", "keyword", keyword),
            RedactionRule.regex("regex.secret", "secret", pattern),
        )
        val policy = RedactionPolicy.of(rules)
        val redactor = TextRedactor.of(policy)
        rules.clear()

        val result = redactor.redact("$keyword secret-123")
        result.redactedText shouldBeEqualTo "${"*".repeat(keyword.length)} **********"
        result.toString() shouldNotContain keyword
        result.toString() shouldNotContain pattern
        policy.toString() shouldNotContain keyword
        policy.toString() shouldNotContain pattern
        rules.size shouldBeEqualTo 0
    }

    @Test
    fun `equal priority uses deterministic rule id and category tie break`() {
        val policy = RedactionPolicy.of(
            rules = listOf(
                RedactionRule.keyword("keyword.beta", "beta", "account", priority = 20),
                RedactionRule.keyword("keyword.alpha", "alpha", "account number", priority = 20),
            )
        )

        val span = TextRedactor.of(policy).redact("account number").spans.single()

        span.category shouldBeEqualTo "alpha"
        span.ruleIds shouldBeEqualTo listOf("keyword.alpha", "keyword.beta")
    }

    @Test
    fun `invalid metadata regex and duplicate ids are rejected safely`() {
        val rawPattern = "(["
        val failure = assertFailsWith<IllegalArgumentException> {
            RedactionRule.regex("regex.invalid", "secret", rawPattern)
        }
        (failure.message ?: "") shouldNotContain rawPattern

        assertFailsWith<IllegalArgumentException> {
            RedactionPolicy.of(
                listOf(
                    RedactionRule.keyword("keyword.same", "keyword", "one"),
                    RedactionRule.keyword("keyword.same", "keyword", "two"),
                )
            )
        }

        val metadata = RedactionRule.keyword("keyword.safe", "safe", "secret")
        metadata.toString() shouldContain "keyword.safe"
        metadata.toString() shouldNotContain "secret"
        metadata.toString().contains("safe").shouldBeTrue()
    }
}
