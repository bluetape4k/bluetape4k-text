package io.bluetape4k.text.search

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldContain
import io.bluetape4k.assertions.shouldHaveSize
import io.bluetape4k.assertions.shouldNotBeEmpty
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
 * [SearchOptions] 매트릭스 검증 테스트.
 *
 * spec §7.2 옵션 조합을 개별 테스트로 커버한다.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AhoCorasickOptionsTest : AbstractAhoCorasickTest() {

    companion object : KLogging()

    // ──────────────────────────────── Test 1 ────────────────────────────────

    @Test
    fun `ignoreCase=false allowOverlaps=true NONE NONE - 모든 매치 포함`() {
        // 준비: 기본 옵션 — 대소문자 구분, 겹침 허용
        val automaton = AhoCorasickAutomaton.builder<String>()
            .add("he", "HE")
            .add("she", "SHE")
            .add("his", "HIS")
            .add("hers", "HERS")
            .options(
                SearchOptions(
                    ignoreCase = false,
                    allowOverlaps = true,
                    wordBoundary = WordBoundary.NONE,
                    normalization = NormalizationForm.NONE,
                )
            )
            .build()

        // 실행
        val matches = automaton.parseText("ushers")

        // 검증: "ushers" 에서 she(1), he(2), hers(2) 가 매치됨
        matches.shouldNotBeEmpty()
        val keywords = matches.map { it.keyword }.toSet()
        keywords shouldContain "she"
        keywords shouldContain "he"
        keywords shouldContain "hers"
        log.debug { "기본 옵션 매치: $matches" }
    }

    // ──────────────────────────────── Test 2 ────────────────────────────────

    @Test
    fun `ignoreCase=true - 대소문자 무시`() {
        // 준비
        val automaton = AhoCorasickAutomaton.builder<String>()
            .add("apple", "APPLE")
            .add("banana", "BANANA")
            .options(SearchOptions(ignoreCase = true))
            .build()

        // 실행
        val matches = automaton.parseText("APPLE and Banana")

        // 검증: 대소문자 무관하게 2건 매치
        matches shouldHaveSize 2
        val keywords = matches.map { it.keyword }.toSet()
        // ignoreCase=true 시 키워드는 소문자로 정규화됨
        keywords shouldContain "apple"
        keywords shouldContain "banana"
        log.debug { "ignoreCase=true 매치: $matches" }
    }

    // ──────────────────────────────── Test 3 ────────────────────────────────

    @Test
    fun `allowOverlaps=false - 더 긴 keyword 우선 (hotel 케이스)`() {
        // 준비: "hot"과 "hotel" 둘 다 등록 — "hotel" 텍스트에서 겹침 제거 시 "hotel"이 우선
        val automaton = AhoCorasickAutomaton.builder<String>()
            .add("hot", "HOT")
            .add("hotel", "HOTEL")
            .options(SearchOptions(allowOverlaps = false))
            .build()

        // 실행
        val matches = automaton.parseText("hotel")

        // 검증: "hotel"만 살아남아야 함 (더 긴 키워드 우선)
        matches shouldHaveSize 1
        matches[0].keyword shouldBeEqualTo "hotel"
        matches[0].value shouldBeEqualTo "HOTEL"
        log.debug { "allowOverlaps=false hotel 케이스: $matches" }
    }

    // ──────────────────────────────── Test 4 ────────────────────────────────

    @Test
    fun `wordBoundary=LATIN_ALPHA - 부분 단어 제외`() {
        // 준비: "apple"을 LATIN_ALPHA 경계로만 매치 — "pineapple" 내부에서는 매치 안 됨
        val automaton = AhoCorasickAutomaton.builder<String>()
            .add("apple", "APPLE")
            .options(SearchOptions(wordBoundary = WordBoundary.LATIN_ALPHA))
            .build()

        // 실행
        val noMatch = automaton.parseText("pineapple")
        val matched = automaton.parseText("eat apple now")

        // 검증: 합성어 내부는 매치 안 됨
        noMatch shouldHaveSize 0

        // 단독 단어는 매치됨
        matched shouldHaveSize 1
        matched[0].keyword shouldBeEqualTo "apple"
        log.debug { "LATIN_ALPHA 경계 검증 — noMatch: $noMatch, matched: $matched" }
    }

    // ──────────────────────────────── Test 5 ────────────────────────────────

    @Test
    fun `wordBoundary=WHITESPACE_SEPARATED - 공백 경계만 매치`() {
        // 준비
        val automaton = AhoCorasickAutomaton.builder<String>()
            .add("run", "RUN")
            .options(SearchOptions(wordBoundary = WordBoundary.WHITESPACE_SEPARATED))
            .build()

        // 실행
        val noMatch = automaton.parseText("running fast")    // 뒤에 문자가 붙어 있음
        val matched = automaton.parseText("please run now")  // 공백으로 분리된 단독 단어

        // 검증
        noMatch shouldHaveSize 0
        matched shouldHaveSize 1
        matched[0].keyword shouldBeEqualTo "run"
        automaton.containsMatch("running fast").shouldBeFalse()
        automaton.containsMatch("please run now").shouldBeTrue()
        log.debug { "WHITESPACE_SEPARATED 경계 검증 — noMatch: $noMatch, matched: $matched" }
    }

    // ──────────────────────────────── Test 6 ────────────────────────────────

    @Test
    fun `stopOnFirstMatch=true - 첫 매치 후 중단`() {
        // 준비
        val automaton = AhoCorasickAutomaton.builder<String>()
            .add("one", "1")
            .add("two", "2")
            .add("three", "3")
            .options(SearchOptions(stopOnFirstMatch = true))
            .build()

        // 실행
        val matches = automaton.parseText("one two three")

        // 검증: stopOnFirstMatch=true이므로 정확히 1건만 반환
        matches shouldHaveSize 1
        matches[0].keyword shouldBeEqualTo "one"
        log.debug { "stopOnFirstMatch=true 결과: $matches" }
    }

    // ──────────────────────────────── Test 7 ────────────────────────────────

    @Test
    fun `stopOnFirstMatch=true는 blocking parseText API에서 첫 매치만 반환함`() {
        // 준비: stopOnFirstMatch=true 옵션으로 automaton 생성
        val automaton = AhoCorasickAutomaton.builder<String>()
            .add("alpha", "ALPHA")
            .add("beta", "BETA")
            .add("gamma", "GAMMA")
            .options(SearchOptions(stopOnFirstMatch = true))
            .build()

        val allOpts = AhoCorasickAutomaton.builder<String>()
            .add("alpha", "ALPHA")
            .add("beta", "BETA")
            .add("gamma", "GAMMA")
            .build()

        val text = "alpha beta gamma"

        // 실행
        val stoppedMatches = automaton.parseText(text)
        val allMatches = allOpts.parseText(text)

        // 검증: stopOnFirstMatch 버전은 1건만 반환
        stoppedMatches shouldHaveSize 1
        // 전체 매치 버전은 3건 반환
        allMatches shouldHaveSize 3
        // 첫 매치는 동일해야 함
        stoppedMatches[0].keyword shouldBeEqualTo allMatches[0].keyword
        log.debug { "stopOnFirstMatch 비교 — stopped: $stoppedMatches, all: $allMatches" }
    }
}
