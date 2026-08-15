package io.bluetape4k.text.search.benchmark

import io.bluetape4k.text.search.AhoCorasickAutomaton
import io.bluetape4k.text.search.AhoCorasickMatch
import io.bluetape4k.text.search.NormalizationForm
import io.bluetape4k.text.search.SearchOptions
import io.bluetape4k.text.search.ahoCorasickOf
import io.bluetape4k.text.search.flow.matchesAsFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.BenchmarkMode
import org.openjdk.jmh.annotations.Fork
import org.openjdk.jmh.annotations.Level
import org.openjdk.jmh.annotations.Measurement
import org.openjdk.jmh.annotations.Mode
import org.openjdk.jmh.annotations.OutputTimeUnit
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.Setup
import org.openjdk.jmh.annotations.State
import org.openjdk.jmh.annotations.Warmup
import java.util.concurrent.TimeUnit

/**
 * Aho-Corasick 자동자의 대표 검색 경로에 대한 JMH 처리량 벤치마크.
 *
 * - [parseTextLargeDictionary]: 큰 사전 + 긴 입력 기준선
 * - [matchesAsFlowLargeDictionaryCollect]: `channelFlow` + `Dispatchers.Default` 기반 스트리밍 검색
 * - [parseTextDenseMatches]: 겹치는 dense match 기준선
 * - [parseTextNoMatch]: 매치가 없는 입력 기준선
 * - [parseTextNfkcNormalization]: NFKC 정규화 경로 기준선
 * - [parseTextNfkcNormalizationLargeInput]: 100,000자 NFKC 정규화 회귀 기준선
 * - [naiveContainsSmallDictionary]: `String.contains` 순차 비교군
 *
 * **벤치마크 설계**:
 * - 사전 크기, 매치 밀도, no-match 입력, Unicode 정규화, Flow 수집 비용을 분리한다.
 * - 처리량 지표는 ops/s이며 높을수록 좋다.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 2, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(value = 1)
open class AhoCorasickBenchmark {

    private lateinit var largeDictionaryMatcher: AhoCorasickAutomaton<String>
    private lateinit var denseMatcher: AhoCorasickAutomaton<String>
    private lateinit var normalizedMatcher: AhoCorasickAutomaton<String>
    private lateinit var smallKeywords: List<String>
    private lateinit var largeDictionaryText: String
    private lateinit var noMatchText: String
    private lateinit var denseText: String
    private lateinit var normalizedText: String
    private lateinit var normalizedLargeText: String

    @Setup(Level.Trial)
    fun setup() {
        smallKeywords = (1..SMALL_KEYWORD_COUNT).map { "keyword$it" }
        val largeKeywords = (1..LARGE_KEYWORD_COUNT).map { "keyword$it" }

        largeDictionaryMatcher = ahoCorasickOf(largeKeywords)
        denseMatcher = ahoCorasickOf(listOf("a", "aa", "aaa", "aaaa", "aaaaa"))
        normalizedMatcher = ahoCorasickOf(
            listOf("cafe", "한글", "(주)"),
            SearchOptions(ignoreCase = true, normalization = NormalizationForm.NFKC),
        )

        largeDictionaryText = buildString {
            repeat(LARGE_TEXT_TOKEN_COUNT) { index ->
                append("payload keyword${(index % LARGE_KEYWORD_COUNT) + 1} value ")
            }
        }
        noMatchText = buildString {
            repeat(LARGE_TEXT_TOKEN_COUNT) { index ->
                append("payload unmatched-$index value ")
            }
        }
        denseText = buildString {
            repeat(DENSE_TOKEN_COUNT) {
                append("aaaaa ")
            }
        }
        normalizedText = buildString {
            repeat(NORMALIZED_TOKEN_COUNT) {
                append("ＣＡＦＥ 한글 ㈜ ")
            }
        }
        normalizedLargeText = "a".repeat(NORMALIZED_LARGE_TEXT_LENGTH)
    }

    /**
     * 큰 사전과 긴 입력에서 [AhoCorasickAutomaton.parseText] 처리량을 측정한다.
     *
     * @return 매치 결과 리스트 (검색 경로 유지용 반환값)
     */
    @Benchmark
    fun parseTextLargeDictionary(): List<AhoCorasickMatch<String>> =
        largeDictionaryMatcher.parseText(largeDictionaryText)

    /**
     * 큰 사전과 긴 입력에서 [matchesAsFlow] 전체 수집 처리량을 측정한다.
     *
     * `channelFlow` + `Dispatchers.Default` 오버헤드를 [parseTextLargeDictionary]와 비교한다.
     *
     * @return 수집된 매치 수 (JMH dead-code 제거 방지용 반환값)
     */
    @Benchmark
    fun matchesAsFlowLargeDictionaryCollect(): Int = runBlocking {
        largeDictionaryMatcher.matchesAsFlow(largeDictionaryText).toList().size
    }

    /**
     * 겹치는 키워드가 많은 dense input에서 overlap 처리량을 측정한다.
     *
     * @return 매치 결과 리스트 (검색 경로 유지용 반환값)
     */
    @Benchmark
    fun parseTextDenseMatches(): List<AhoCorasickMatch<String>> =
        denseMatcher.parseText(denseText)

    /**
     * 매치가 없는 긴 입력에서 실패 탐색 비용을 측정한다.
     *
     * @return 매치 결과 리스트 (검색 경로 유지용 반환값)
     */
    @Benchmark
    fun parseTextNoMatch(): List<AhoCorasickMatch<String>> =
        largeDictionaryMatcher.parseText(noMatchText)

    /**
     * NFKC 정규화와 대소문자 무시 옵션을 함께 사용하는 경로의 처리량을 측정한다.
     *
     * @return 매치 결과 리스트 (검색 경로 유지용 반환값)
     */
    @Benchmark
    fun parseTextNfkcNormalization(): List<AhoCorasickMatch<String>> =
        normalizedMatcher.parseText(normalizedText)

    /**
     * 100,000자 ASCII 입력에서 NFKC 정규화와 검색을 수행하는 처리량을 측정한다.
     *
     * 입력 크기를 고정해 [io.bluetape4k.text.search.internal.OffsetMapping]의 prefix 재정규화 회귀가
     * benchmark 결과에 드러나도록 한다.
     *
     * @return 매치 결과 리스트 (검색 경로 유지용 반환값)
     */
    @Benchmark
    fun parseTextNfkcNormalizationLargeInput(): List<AhoCorasickMatch<String>> =
        normalizedMatcher.parseText(normalizedLargeText)

    /**
     * 작은 사전 기준 순진한(naive) `String.contains` 순차 비교 처리량을 측정한다.
     *
     * O(k × n) 복잡도로 키워드 수(k)와 텍스트 길이(n)에 비례한다.
     *
     * @return 매치 횟수 (JMH dead-code 제거 방지용 반환값)
     */
    @Benchmark
    fun naiveContainsSmallDictionary(): Int =
        smallKeywords.count { largeDictionaryText.contains(it) }

    companion object {
        private const val SMALL_KEYWORD_COUNT = 1_000
        private const val LARGE_KEYWORD_COUNT = 5_000
        private const val LARGE_TEXT_TOKEN_COUNT = 2_000
        private const val DENSE_TOKEN_COUNT = 2_000
        private const val NORMALIZED_TOKEN_COUNT = 1_000
        private const val NORMALIZED_LARGE_TEXT_LENGTH = 100_000
    }
}
