package io.bluetape4k.text.search

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldHaveSize
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
 * [AhoCorasickAutomaton.firstMatch] R5 leftmost-longest 의미론을 검증하는 테스트.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AhoCorasickFirstMatchTest : AbstractAhoCorasickTest() {

    companion object : KLogging()

    // ──────────────────────────────── Test 1 ────────────────────────────────

    @Test
    fun `ushers + he she hers allowOverlaps=true - firstMatch는 she (start=1)`() {
        // 준비: allowOverlaps=true로 "ushers"에서 she(start=1), he(start=2), hers(start=2) 모두 매치
        val automaton = AhoCorasickAutomaton.builder<String>()
            .add("he", "HE")
            .add("she", "SHE")
            .add("hers", "HERS")
            .options(SearchOptions(allowOverlaps = true))
            .build()

        // 실행
        val first = automaton.firstMatch("ushers")

        // 검증: leftmost 기준 she(start=1)이 he/hers(start=2)보다 앞섬
        first.shouldNotBeNull()
        first.keyword shouldBeEqualTo "she"
        first.start shouldBeEqualTo 1
        first.end shouldBeEqualTo 3
        log.debug { "ushers firstMatch: $first" }
    }

    // ──────────────────────────────── Test 2 ────────────────────────────────

    @Test
    fun `동일 start이면 더 긴 keyword 우선`() {
        // 준비: "he"와 "hers" 둘 다 start=0 에서 시작 ("hers" 텍스트)
        val automaton = AhoCorasickAutomaton.builder<String>()
            .add("he", "HE")
            .add("hers", "HERS")
            .options(SearchOptions(allowOverlaps = true))
            .build()

        // 실행
        val first = automaton.firstMatch("hers")

        // 검증: start=0으로 동일 — 더 긴 "hers"(length=4) 가 "he"(length=2) 보다 우선
        first.shouldNotBeNull()
        first.keyword shouldBeEqualTo "hers"
        first.start shouldBeEqualTo 0
        first.end shouldBeEqualTo 3
        first.length shouldBeEqualTo 4
        log.debug { "동일 start 더 긴 keyword 우선: $first" }
    }

    @Test
    fun `stopOnFirstMatch=true에서도 firstMatch는 동일 start의 longest keyword를 반환한다`() {
        // 준비: 짧은 keyword가 먼저 emit되더라도 firstMatch는 leftmost-longest 계약을 유지해야 한다.
        val automaton = AhoCorasickAutomaton.builder<String>()
            .add("he", "HE")
            .add("hers", "HERS")
            .options(SearchOptions(stopOnFirstMatch = true))
            .build()

        // 실행
        val first = automaton.firstMatch("hers")

        // 검증: stopOnFirstMatch는 parseText의 결과 수만 제한하고 firstMatch 선택에는 영향을 주지 않는다.
        first.shouldNotBeNull()
        first.keyword shouldBeEqualTo "hers"
        first.start shouldBeEqualTo 0
        first.length shouldBeEqualTo 4
    }

    @Test
    fun `stopOnFirstMatch=true와 allowOverlaps=false에서도 longest keyword를 반환한다`() {
        // 준비: 겹침 제거 경로도 모든 후보를 본 뒤 leftmost-longest를 선택해야 한다.
        val automaton = AhoCorasickAutomaton.builder<String>()
            .add("hot", "HOT")
            .add("hotel", "HOTEL")
            .options(SearchOptions(allowOverlaps = false, stopOnFirstMatch = true))
            .build()

        // 실행 및 검증
        val first = automaton.firstMatch("hotel")
        first.shouldNotBeNull()
        first.keyword shouldBeEqualTo "hotel"
        first.length shouldBeEqualTo 5
    }

    @Test
    fun `stopOnFirstMatch=true와 whitespace boundary에서 뒤의 유효한 match를 찾는다`() {
        // 준비: 첫 raw emit은 부분 문자열이므로 탈락하고, 뒤의 독립 단어는 유효하다.
        val automaton = AhoCorasickAutomaton.builder<String>()
            .add("run", "RUN")
            .options(
                SearchOptions(
                    wordBoundary = WordBoundary.WHITESPACE_SEPARATED,
                    stopOnFirstMatch = true,
                )
            )
            .build()

        // 실행 및 검증
        val first = automaton.firstMatch("running run")
        first.shouldNotBeNull()
        first.keyword shouldBeEqualTo "run"
        first.start shouldBeEqualTo 8
    }

    // ──────────────────────────────── Test 3 ────────────────────────────────

    @Test
    fun `매치 없음 - firstMatch는 null`() {
        // 준비
        val automaton = AhoCorasickAutomaton.builder<String>()
            .add("apple", "APPLE")
            .add("banana", "BANANA")
            .build()

        // 실행
        val first = automaton.firstMatch("no keywords here")

        // 검증
        first.shouldBeNull()
        log.debug { "매치 없음 → firstMatch=null 검증 완료" }
    }

    // ──────────────────────────────── Test 4 ────────────────────────────────

    @Test
    fun `allowOverlaps=false와 true의 firstMatch 동작 비교`() {
        // 준비
        // "hot", "hotel" — allowOverlaps=false 시 "hotel"만 남음
        val automatonOverlap = AhoCorasickAutomaton.builder<String>()
            .add("hot", "HOT")
            .add("hotel", "HOTEL")
            .options(SearchOptions(allowOverlaps = true))
            .build()

        val automatonNoOverlap = AhoCorasickAutomaton.builder<String>()
            .add("hot", "HOT")
            .add("hotel", "HOTEL")
            .options(SearchOptions(allowOverlaps = false))
            .build()

        val text = "hotel"

        // 실행
        val firstWithOverlap = automatonOverlap.firstMatch(text)
        val firstNoOverlap = automatonNoOverlap.firstMatch(text)

        // 검증
        // allowOverlaps=true: "hot"(start=0, len=3)과 "hotel"(start=0, len=5) 중 더 긴 "hotel" 우선
        firstWithOverlap.shouldNotBeNull()
        firstWithOverlap.keyword shouldBeEqualTo "hotel"

        // allowOverlaps=false: IntervalTree가 겹침 제거 → "hotel"만 남음
        firstNoOverlap.shouldNotBeNull()
        firstNoOverlap.keyword shouldBeEqualTo "hotel"

        // 두 결과 모두 "hotel"을 가리켜야 함
        firstWithOverlap.keyword shouldBeEqualTo firstNoOverlap.keyword
        firstWithOverlap.start shouldBeEqualTo firstNoOverlap.start

        log.debug { "allowOverlaps 비교 — 겹침 허용: $firstWithOverlap, 겹침 제거: $firstNoOverlap" }
    }

    @Test
    fun `ignoreCase Unicode keyword와 text는 동일한 case fold pipeline을 사용한다`() {
        val automaton = AhoCorasickAutomaton.builder<Unit>()
            .add("İ", Unit)
            .add("ΟΣ", Unit)
            .add("ПРИВЕТ", Unit)
            .options(SearchOptions(ignoreCase = true))
            .build()
        val text = "İ ΟΣ ПРИВЕТ"

        val matches = automaton.parseText(text)
        matches shouldHaveSize 3
        matches.map { it.start } shouldBeEqualTo listOf(0, 2, 5)
        matches.map { it.end } shouldBeEqualTo listOf(0, 3, 10)

        val combiningDotAutomaton = AhoCorasickAutomaton.builder<Unit>()
            .add("I\u0307", Unit)
            .options(SearchOptions(ignoreCase = true, normalization = NormalizationForm.NONE))
            .build()
        val combiningDotMatches = combiningDotAutomaton.parseText("i\u0307")
        combiningDotMatches shouldHaveSize 1
        combiningDotMatches.single().keyword shouldBeEqualTo "i\u0307"
        combiningDotMatches.single().start shouldBeEqualTo 0
        combiningDotMatches.single().end shouldBeEqualTo 1

        val expanded = AhoCorasickAutomaton.builder<Unit>()
            .add("İ", Unit)
            .options(SearchOptions(ignoreCase = true))
            .build()
            .parseText("xİy")
        expanded shouldHaveSize 1
        expanded.single().keyword shouldBeEqualTo "i\u0307"
        expanded.single().start shouldBeEqualTo 1
        expanded.single().end shouldBeEqualTo 1

        val deseretUpper = String(Character.toChars(0x10400))
        val deseretLower = String(Character.toChars(0x10428))
        val deseret = AhoCorasickAutomaton.builder<Unit>()
            .add(deseretUpper, Unit)
            .options(SearchOptions(ignoreCase = true))
            .build()
            .parseText("x${deseretLower}y")
        deseret shouldHaveSize 1
        deseret.single().start shouldBeEqualTo 1
        deseret.single().end shouldBeEqualTo 2

        val first = automaton.firstMatch(text)
        first.shouldNotBeNull()
        first.start shouldBeEqualTo 0
        first.end shouldBeEqualTo 0
        automaton.containsMatch("ΟΣ").shouldBeTrue()
    }
}
