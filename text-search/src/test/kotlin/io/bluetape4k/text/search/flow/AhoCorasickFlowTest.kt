package io.bluetape4k.text.search.flow

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEmpty
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeInstanceOf
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldHaveSize
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.text.search.NormalizationForm
import io.bluetape4k.text.search.SearchOptions
import io.bluetape4k.text.search.WordBoundary
import io.bluetape4k.text.search.ahoCorasickOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
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
import java.util.concurrent.atomic.AtomicBoolean
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
        private const val EAGER_CANCELLATION_TEXT_SIZE = 50_000_000
        private val EAGER_CANCELLATION_TEXT = "x".repeat(EAGER_CANCELLATION_TEXT_SIZE)
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
    fun `Unicode ignoreCase Flow도 keyword와 동일한 pipeline을 사용한다`() = runTest(timeout = 30.seconds) {
        val automaton = ahoCorasickOf(
            "İ",
            "ΟΣ",
            "ПРИВЕТ",
            options = SearchOptions(ignoreCase = true),
        )

        val matches = automaton.matchesAsFlow("İ ΟΣ ПРИВЕТ").toList()

        matches shouldHaveSize 3
        matches.map { it.start } shouldBeEqualTo listOf(0, 2, 5)
        matches.map { it.end } shouldBeEqualTo listOf(0, 3, 10)

        val combiningDotAutomaton = ahoCorasickOf(
            "I\u0307",
            options = SearchOptions(ignoreCase = true, normalization = NormalizationForm.NONE),
        )
        val combiningDotMatches = combiningDotAutomaton.matchesAsFlow("i\u0307").toList()
        combiningDotMatches shouldHaveSize 1
        combiningDotMatches.single().keyword shouldBeEqualTo "i\u0307"
        combiningDotMatches.single().start shouldBeEqualTo 0
        combiningDotMatches.single().end shouldBeEqualTo 1
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
        val eagerMatches = automaton.parseText(SAMPLE_TEXT)

        eagerMatches shouldBeEqualTo matches
        matches.map { Triple(it.start, it.end, it.keyword) } shouldBeEqualTo listOf(
            Triple(2, 5, "hers"),
        )
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

        // 검증: Flow는 stopOnFirstMatch를 무시하고 raw match 전체를 방출하며 take(1)은 첫 항목만 남긴다.
        allFromFlow.map { it.keyword } shouldBeEqualTo listOf("he", "she", "hers")
        firstFromFlow shouldHaveSize 1
        allFromFlow.first() shouldBeEqualTo firstFromFlow.single()
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
        flowMatches.map { it.keyword } shouldBeEqualTo listOf("he", "she", "hers")
    }

    @Test
    fun `기본 옵션에서는 synchronous parseText와 Flow 결과 순서가 같다`() = runTest(timeout = 30.seconds) {
        val automaton = fixtureAutomaton()

        val eagerMatches = automaton.parseText(SAMPLE_TEXT)
        val flowMatches = automaton.matchesAsFlow(SAMPLE_TEXT).toList()

        eagerMatches shouldBeEqualTo flowMatches
    }

    @Test
    fun `1만 매치 throughput micro-test`() = runTest(timeout = 30.seconds) {
        // 준비: 키워드 100개 + 동일 텍스트 100번 반복 → 다수의 매치 생성
        val keywords = (0 until 100).map { "keyword${it.toString().padStart(3, '0')}" }
        val automaton = ahoCorasickOf(keywords)
        val text = buildString {
            repeat(100) {
                keywords.forEach { append(it).append(' ') }
            }
        }

        // 실행
        val matches = automaton.matchesAsFlow(text).toList()

        // 검증: 100 keywords × 100 repeats = 정확히 10,000건
        matches shouldHaveSize 10_000
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
    fun `첫 match 이전의 대규모 no-match 순회도 취소를 관찰한다`() = runSuspendIO {
        val started = CompletableDeferred<Unit>()
        val release = AtomicBoolean(false)
        val text = object: CharSequence {
            private val size = 10_000_000

            override val length: Int get() = size

            override fun get(index: Int): Char = 'x'

            override fun subSequence(startIndex: Int, endIndex: Int): CharSequence = this

            override fun toString(): String {
                started.complete(Unit)
                while (!release.get()) Thread.yield()
                return "x".repeat(size)
            }
        }

        val producer = async(Dispatchers.Default) {
            fixtureAutomaton().matchesAsFlow(text).toList()
        }

        try {
            withTimeout(5.seconds) { started.await() }
            producer.cancel()
            release.set(true)
            withTimeout(5.seconds) { producer.join() }
            producer.isCancelled.shouldBeTrue()
        } finally {
            release.set(true)
            producer.cancelAndJoin()
        }
    }

    @Test
    fun `allowOverlaps=false 대규모 no-match eager 순회는 취소를 관찰한다`() = runSuspendIO {
        assertEagerNoMatchCancellation(SearchOptions(allowOverlaps = false))
    }

    @Test
    fun `wordBoundary eager 대규모 no-match 순회는 취소를 관찰한다`() = runSuspendIO {
        assertEagerNoMatchCancellation(SearchOptions(wordBoundary = WordBoundary.LATIN_ALPHA))
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

    private suspend fun assertEagerNoMatchCancellation(options: SearchOptions) = coroutineScope {
        val started = CompletableDeferred<Unit>()
        val text = SignallingNoMatchText(EAGER_CANCELLATION_TEXT, started)
        val producer = async(Dispatchers.Default) {
            ahoCorasickOf("needle", options = options).matchesAsFlow(text).toList()
        }

        try {
            withTimeout(5.seconds) { started.await() }
            producer.cancel()
            withTimeout(5.seconds) { producer.join() }
            producer.isCancelled.shouldBeTrue()
        } finally {
            producer.cancelAndJoin()
        }
    }

    private class SignallingNoMatchText(
        private val value: String,
        private val started: CompletableDeferred<Unit>,
    ) : CharSequence {
        override val length: Int get() = value.length

        override fun get(index: Int): Char = value[index]

        override fun subSequence(startIndex: Int, endIndex: Int): CharSequence = value.subSequence(startIndex, endIndex)

        override fun toString(): String {
            started.complete(Unit)
            return value
        }
    }

    private class FailingCharSequence : CharSequence {
        override val length: Int = 1

        override fun get(index: Int): Char = 'x'

        override fun subSequence(startIndex: Int, endIndex: Int): CharSequence = this

        override fun toString(): String = error("synthetic upstream failure")
    }
}
