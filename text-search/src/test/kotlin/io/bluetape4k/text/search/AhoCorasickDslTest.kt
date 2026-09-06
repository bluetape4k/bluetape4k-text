package io.bluetape4k.text.search

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldContainAll
import io.bluetape4k.assertions.shouldHaveSize
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AhoCorasickDslTest {

    companion object : KLogging()

    @Test
    fun `DSL로 옵션과 키워드 5개 등록 후 parseText 결과 검증`() {
        // 준비
        val automaton = ahoCorasick<String> {
            ignoreCase = true
            keyword("apple", "APPLE")
            keyword("banana", "BANANA")
            keyword("cherry", "CHERRY")
            keyword("date", "DATE")
            keyword("elderberry", "ELDERBERRY")
        }

        // 실행
        val matches = automaton.parseText("I have Apple, Banana, Cherry, Date and Elderberry")

        // 검증
        matches shouldHaveSize 5
        val keywords = matches.map { it.value }
        keywords shouldContainAll listOf("APPLE", "BANANA", "CHERRY", "DATE", "ELDERBERRY")
        log.debug { "DSL 매치 결과: $matches" }
    }

    @Test
    fun `ahoCorasickOf vararg 헬퍼에서 keyword==value 검증`() {
        // 준비
        val automaton = ahoCorasickOf("foo", "bar", "baz")

        // 실행
        val matches = automaton.parseText("foo and bar and baz")

        // 검증
        matches shouldHaveSize 3
        matches.forEach { match ->
            // keyword와 value가 동일해야 함
            match.keyword shouldBeEqualTo match.value
        }
        log.debug { "vararg 헬퍼 결과: $matches" }
    }

    @Test
    fun `Map으로 keyword 등록 후 모든 키워드 매치 검증`() {
        // 준비
        val keywordMap = mapOf(
            "NYC" to "New York City",
            "LA" to "Los Angeles",
            "SF" to "San Francisco",
        )
        val automaton = ahoCorasick<String> {
            keywords(keywordMap)
        }

        // 실행
        val matches = automaton.parseText("Visiting NYC, LA and SF this summer")

        // 검증
        matches shouldHaveSize 3
        val valueMap = matches.associate { it.keyword to it.value }
        valueMap["NYC"] shouldBeEqualTo "New York City"
        valueMap["LA"] shouldBeEqualTo "Los Angeles"
        valueMap["SF"] shouldBeEqualTo "San Francisco"
        log.debug { "Map 키워드 등록 결과: $matches" }
    }

    @Test
    fun `4가지 옵션 동시 적용 - ignoreCase + wordBoundary + allowOverlaps + stopOnFirstMatch`() {
        // 준비
        // ignoreCase=true, wordBoundary=WHITESPACE_SEPARATED, allowOverlaps=true, stopOnFirstMatch=true
        // "He" substring이 포함된 "ushers"는 WHITESPACE_SEPARATED 경계로 인해 매치 안 됨
        // 단독 단어인 "he"만 매치되어야 함
        val automaton = ahoCorasick<String> {
            ignoreCase = true
            wordBoundary = WordBoundary.WHITESPACE_SEPARATED
            allowOverlaps = true
            stopOnFirstMatch = true
            keyword("he", "HE")
            keyword("she", "SHE")
        }

        // 실행
        // "ushers"에 포함된 "she"/"he"는 단어 경계가 없어서 매치 안 됨
        // 단독 단어 "He"는 ignoreCase로 매치
        val matches = automaton.parseText("He likes ushers she said")

        // 검증: stopOnFirstMatch=true이므로 정확히 1개만 반환
        matches shouldHaveSize 1
        val first = matches.first()
        first.shouldNotBeNull()
        first.value shouldBeEqualTo "HE"
        log.debug { "4-옵션 동시 적용 결과: $matches" }
    }

    @Test
    fun `blank keyword 등록 시 IllegalArgumentException 발생`() {
        // 실행 및 검증
        assertFailsWith<IllegalArgumentException> {
            ahoCorasick<String> {
                keyword("  ", "value")
            }
        }
    }

    @Test
    fun `DSL build - normalization collision을 조용히 덮어쓰지 않고 거부`() {
        // 실행 및 검증: DSL도 동일한 Builder 충돌 정책을 적용한다.
        val failure = assertFailsWith<IllegalArgumentException> {
            ahoCorasick<String> {
                ignoreCase = true
                keyword("A", "upper")
                keyword("a", "lower")
            }
        }
        failure.message.orEmpty() shouldBeEqualTo
            "Normalization collision: keywords 'A' and 'a' both normalize to 'a'"
    }
}
