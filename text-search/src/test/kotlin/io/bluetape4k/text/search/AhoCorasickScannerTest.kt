package io.bluetape4k.text.search

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldHaveSize
import org.junit.jupiter.api.Test

/** 청크 경계, offset, 종료 처리 및 옵션 호환성을 검증합니다. */
class AhoCorasickScannerTest: AbstractAhoCorasickTest() {

    @Test
    fun `키워드가 여러 청크에 걸쳐도 finish에서 원본 offset을 유지한다`() {
        val automaton = AhoCorasickAutomaton.builder<String>()
            .add("secret", "SECRET")
            .add("and", "AND")
            .build()
        val scanner = automaton.scanner()

        val streamed = buildList {
            addAll(scanner.scan("se"))
            addAll(scanner.scan("cr"))
            addAll(scanner.scan("et and"))
            addAll(scanner.finish())
        }

        streamed.map { Triple(it.start, it.end, it.keyword) } shouldBeEqualTo listOf(
            Triple(0, 5, "secret"),
            Triple(7, 9, "and"),
        )
    }

    @Test
    fun `충분히 앞선 결과는 scan에서 방출하고 중복하지 않는다`() {
        val automaton = AhoCorasickAutomaton.builder<String>()
            .add("abc", "ABC")
            .build()
        val scanner = automaton.scanner()

        val first = scanner.scan("abc---abc")
        val second = scanner.scan("---tail")
        val last = scanner.finish()

        first.map { it.start } shouldBeEqualTo listOf(0)
        (second + last).map { it.start } shouldBeEqualTo listOf(6)
    }

    @Test
    fun `scanner는 eager 정렬 없이 chunk의 raw traversal 순서를 유지한다`() {
        val automaton = AhoCorasickAutomaton.builder<String>()
            .add("a", "A")
            .add("ba", "BA")
            .build()
        val scanner = automaton.scanner()

        val matches = scanner.scan("ba") + scanner.finish()

        matches.map { it.keyword } shouldBeEqualTo listOf("a", "ba")
        matches.map { it.start } shouldBeEqualTo listOf(1, 0)
    }

    @Test
    fun `ignoreCase와 word boundary 옵션을 유지한다`() {
        val scanner = AhoCorasickAutomaton.builder<String>()
            .add("hello", "HELLO")
            .options(
                SearchOptions(
                    ignoreCase = true,
                    wordBoundary = WordBoundary.WHITESPACE_SEPARATED,
                )
            )
            .build()
            .scanner()

        val matches = scanner.scan("HELLO helloish ") + scanner.finish()

        matches.shouldHaveSize(1)
        matches.single().value shouldBeEqualTo "HELLO"
        matches.single().start shouldBeEqualTo 0
    }

    @Test
    fun `정규화 옵션은 streaming scanner에서 명시적으로 거부한다`() {
        assertFailsWith<IllegalArgumentException> {
            AhoCorasickAutomaton.builder<String>()
                .add("가", "GA")
                .options(SearchOptions(normalization = NormalizationForm.NFC))
                .build()
                .scanner()
        }
    }

    @Test
    fun `reset은 새 입력 스트림의 offset을 0부터 시작한다`() {
        val scanner = AhoCorasickAutomaton.builder<String>().add("go", "GO").build().scanner()
        scanner.scan("go")
        scanner.finish()
        scanner.reset()

        val matches = scanner.scan("go") + scanner.finish()

        matches.single().start shouldBeEqualTo 0
    }
}
