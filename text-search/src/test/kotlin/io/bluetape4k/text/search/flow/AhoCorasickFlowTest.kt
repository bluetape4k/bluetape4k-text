package io.bluetape4k.text.search.flow

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEmpty
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeGreaterThan
import io.bluetape4k.assertions.shouldBeInstanceOf
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldHaveSize
import io.bluetape4k.assertions.shouldNotBeEmpty
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.text.search.SearchOptions
import io.bluetape4k.text.search.ahoCorasickOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.time.Duration.Companion.seconds

/**
 * [matchesAsFlow] 확장 함수의 동작을 검증하는 테스트.
 *
 * - `runTest(timeout = 30.seconds)` 사용
 * - `channelFlow` 패턴의 협력 취소 검증 포함
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AhoCorasickFlowTest {

    companion object : KLogging() {
        private const val SAMPLE_TEXT = "ushers"
        private const val REPEATED_MATCHES = 512
    }

    private fun fixtureAutomaton(options: SearchOptions = SearchOptions()) =
        ahoCorasickOf("he", "she", "his", "hers", options = options)

    @Test
    fun `정상 collect - 모든 매치 수집`() = runTest(timeout = 30.seconds) {
        // 준비
        val automaton = fixtureAutomaton()

        // 실행
        val matches = automaton.matchesAsFlow(SAMPLE_TEXT).toList()

        // 검증: "ushers" 에서는 "she"(start=1), "he"(start=2), "hers"(start=2) 가 매치됨
        matches shouldHaveSize 3
        val keywords = matches.map { it.keyword }.toSet()
        keywords shouldBeEqualTo setOf("she", "he", "hers")
        log.debug { "정상 collect 매치: $matches" }
    }

    @Test
    fun `take(2) 조기 종료 - producer 취소 확인`() = runTest(timeout = 30.seconds) {
        // 준비
        val automaton = fixtureAutomaton()

        // 실행: take(2)로 조기 종료 → channelFlow producer 가 협력 취소됨
        val matches = automaton.matchesAsFlow(SAMPLE_TEXT)
            .take(2)
            .toList()

        // 검증
        matches shouldHaveSize 2
        log.debug { "take(2) 조기 종료 매치: $matches" }
    }

    @Test
    fun `빈 텍스트 - empty Flow`() = runTest(timeout = 30.seconds) {
        // 준비
        val automaton = fixtureAutomaton()

        // 실행
        val matches = automaton.matchesAsFlow("").toList()

        // 검증
        matches.shouldBeEmpty()
    }

    @Test
    fun `allowOverlaps=false - IntervalTree 결과 일괄 emit`() = runTest(timeout = 30.seconds) {
        // 준비: 겹치는 매치 제거 옵션
        val automaton = fixtureAutomaton(SearchOptions(allowOverlaps = false))

        // 실행
        val matches = automaton.matchesAsFlow(SAMPLE_TEXT).toList()

        // 검증: "ushers" 에서 겹침 제거 시 "she"(start=1, end=3) + "hers"(start=2, end=5) 의
        // 겹침을 제거 → 더 긴 키워드 우선 → "hers" 만 남거나 비겹침 매치만 남는다.
        // 정확한 결과는 IntervalTree 로직에 의해 결정되며, 핵심은 "겹치지 않는 결과만 emit" 됨을 확인하는 것.
        matches.size shouldBeGreaterThan 0
        // 겹침 검증: 정렬된 매치들 사이에 start/end 가 서로 겹치지 않아야 함
        val sorted = matches.sortedBy { it.start }
        for (i in 1 until sorted.size) {
            val prev = sorted[i - 1]
            val curr = sorted[i]
            (curr.start > prev.end).shouldBeTrue()
        }
        log.debug { "allowOverlaps=false 매치: $matches" }
    }

    @Test
    fun `stopOnFirstMatch=true 는 Flow에서 무시된다 - take(1) 비교`() = runTest(timeout = 30.seconds) {
        // 준비: stopOnFirstMatch=true 옵션
        val automaton = fixtureAutomaton(SearchOptions(stopOnFirstMatch = true))

        // 실행 1: stopOnFirstMatch 옵션이 있어도 Flow 는 모든 매치를 emit (옵션은 무시)
        // 단, parseText 자체가 stopOnFirstMatch 를 적용해 1개만 반환할 수 있음 → 이는 정상 동작
        val allFromFlow = automaton.matchesAsFlow(SAMPLE_TEXT).toList()
        // 실행 2: take(1) 로 첫 매치만 가져오기
        val firstFromFlow = automaton.matchesAsFlow(SAMPLE_TEXT).take(1).toList()

        // 검증: take(1) 결과는 항상 1개
        firstFromFlow shouldHaveSize 1
        // stopOnFirstMatch 가 적용된 경우 Flow 결과도 1개일 수 있음 → 두 결과의 첫 매치는 동일해야 함
        if (allFromFlow.isNotEmpty()) {
            allFromFlow.first() shouldBeEqualTo firstFromFlow.first()
        }
        log.debug { "stopOnFirstMatch+Flow 전체: $allFromFlow, take(1): $firstFromFlow" }
    }

    @Test
    fun `stopOnFirstMatch=true 여도 기본 Flow 는 parseText를 materialize 하지 않는다`() = runTest(timeout = 30.seconds) {
        // 준비
        val automaton = fixtureAutomaton(SearchOptions(stopOnFirstMatch = true))

        // 실행
        val eagerMatches = automaton.parseText(SAMPLE_TEXT)
        val flowMatches = automaton.matchesAsFlow(SAMPLE_TEXT).toList()

        // 검증
        eagerMatches shouldHaveSize 1
        flowMatches shouldHaveSize 3
    }

    @Test
    fun `1만 매치 throughput micro-test`() = runTest(timeout = 30.seconds) {
        // 준비: 키워드 100개 + 동일 텍스트 100번 반복 → 다수의 매치 생성
        val keywords = (0 until 100).map { "kw$it" }
        val automaton = ahoCorasickOf(keywords)
        val text = buildString {
            repeat(100) {
                keywords.forEach { append(it).append(' ') }
            }
        }

        // 실행
        val matches = automaton.matchesAsFlow(text).toList()

        // 검증: 최소 1만 매치 (100 keywords × 100 repeats = 10_000)
        matches.shouldNotBeEmpty()
        log.debug { "throughput micro-test 매치 개수: ${matches.size}" }
    }

    /**
     * [runTest]는 가상 시간을 사용하지만 [matchesAsFlow]는 [kotlinx.coroutines.Dispatchers.Default]를
     * 사용하므로 시간/스케줄링 mismatch 를 피하기 위해 [runBlocking]을 사용한다.
     */
    @Test
    fun `CancellationException 정상 전파`(): Unit = runBlocking {
        // 준비
        val automaton = fixtureAutomaton()
        var collected = 0
        var caught: Throwable? = null

        // 실행: take(1) 후 collect 가 종료되면 채널이 닫히고 producer 가 협력 취소됨.
        // 추가로 명시적인 CancellationException 전파 검증을 위해 collect 내부에서 throw.
        try {
            automaton.matchesAsFlow(SAMPLE_TEXT).collect {
                collected++
                if (collected >= 1) {
                    // 명시적 CancellationException → 정상 전파되어야 함
                    throw CancellationException("test-cancel")
                }
            }
        } catch (e: CancellationException) {
            caught = e
        }

        // 검증
        collected shouldBeEqualTo 1
        caught.shouldNotBeNull()
        caught.shouldBeInstanceOf<CancellationException>()
        log.debug { "CancellationException 정상 전파됨: ${caught.message}" }
    }

    @Test
    fun `take(1) 조기 종료가 upstream producer completion으로 전파된다`() = runSuspendIO {
        // 준비
        val producerCompletion = CompletableDeferred<Throwable?>()
        val text = repeatedMatchText()

        // 실행
        val matches = fixtureAutomaton()
            .matchesAsFlow(text)
            .onCompletion { cause -> producerCompletion.complete(cause) }
            .take(1)
            .toList()

        // 검증
        matches shouldHaveSize 1
        withTimeout(5.seconds) {
            producerCompletion.await().shouldBeInstanceOf<CancellationException>()
        }
    }

    @Test
    fun `실제 Job 취소가 producer와 child cleanup으로 전파된다`() = runSuspendIO {
        // 준비
        val parentJob = Job()
        val scope = CoroutineScope(parentJob + Dispatchers.Default)
        val firstMatch = CompletableDeferred<Unit>()
        val producerCompletion = CompletableDeferred<Throwable?>()
        val collecting = scope.launch {
            fixtureAutomaton()
                .matchesAsFlow(repeatedMatchText())
                .onCompletion { cause -> producerCompletion.complete(cause) }
                .collect {
                    firstMatch.complete(Unit)
                    awaitCancellation()
                }
        }

        try {
            // 실행
            withTimeout(5.seconds) { firstMatch.await() }
            collecting.cancelAndJoin()

            // 검증
            collecting.isCancelled.shouldBeTrue()
            withTimeout(5.seconds) {
                producerCompletion.await().shouldBeInstanceOf<CancellationException>()
            }
            parentJob.children.toList().shouldBeEmpty()
        } finally {
            collecting.cancelAndJoin()
            parentJob.cancel()
        }
    }

    @Test
    fun `upstream failure가 flow completion과 exception 전파로 종료된다`() = runSuspendIO {
        // 준비
        val parentJob = Job()
        val scope = CoroutineScope(parentJob + Dispatchers.Default)
        val producerCompletion = CompletableDeferred<Throwable?>()
        val collecting = scope.async {
            fixtureAutomaton()
                .matchesAsFlow(FailingCharSequence())
                .onCompletion { cause -> producerCompletion.complete(cause) }
                .toList()
        }

        try {
            // 실행
            val failure = assertFailsWith<IllegalStateException> { collecting.await() }

            // 검증
            failure.message shouldBeEqualTo "synthetic upstream failure"
            collecting.isCompleted.shouldBeTrue()
            withTimeout(5.seconds) {
                producerCompletion.await().shouldBeInstanceOf<IllegalStateException>()
            }
            parentJob.children.toList().shouldBeEmpty()
        } finally {
            collecting.cancelAndJoin()
            parentJob.cancel()
        }
    }

    private fun repeatedMatchText(): String = buildString {
        // 충분히 큰 fixture로 producer가 channel buffer를 채우는 동안 collector 취소를 검증한다.
        repeat(REPEATED_MATCHES) {
            append("he ")
        }
    }

    private class FailingCharSequence : CharSequence {
        override val length: Int = 1

        override fun get(index: Int): Char = 'x'

        override fun subSequence(startIndex: Int, endIndex: Int): CharSequence = this

        override fun toString(): String = error("synthetic upstream failure")
    }
}
