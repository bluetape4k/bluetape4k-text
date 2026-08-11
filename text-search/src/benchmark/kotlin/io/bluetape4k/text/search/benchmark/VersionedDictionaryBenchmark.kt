package io.bluetape4k.text.search.benchmark

import io.bluetape4k.tokenizer.korean.utils.KoreanDictionaryProvider
import io.bluetape4k.tokenizer.korean.utils.KoreanPos
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Noun
import io.bluetape4k.tokenizer.utils.DictionaryVersion
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.BenchmarkMode
import org.openjdk.jmh.annotations.Fork
import org.openjdk.jmh.annotations.Level
import org.openjdk.jmh.annotations.Measurement
import org.openjdk.jmh.annotations.Mode
import org.openjdk.jmh.annotations.OperationsPerInvocation
import org.openjdk.jmh.annotations.OutputTimeUnit
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.Setup
import org.openjdk.jmh.annotations.State
import org.openjdk.jmh.annotations.TearDown
import org.openjdk.jmh.annotations.Warmup
import java.util.concurrent.TimeUnit

/**
 * 한국어 production dictionary mutation의 snapshot 복사 비용을 비교하는 JMH 기준선입니다.
 *
 * - [addRemoveWithCopyOnWrite]는 provider의 변경 entry COW 경로를 호출합니다.
 * - [addRemoveWithFullReplacement]는 같은 A→B→A workload를 전체 replacement 경로로
 *   수행합니다.
 *
 * 각 invocation은 Noun-sized set에 고유 단어를 추가한 뒤 제거해 cardinality를 복원합니다.
 * 처리량은 pair 내부 두 mutation을 기준으로 정규화하며, 결과는 운영 성능 순위가 아닌
 * 동일 환경의 로컬 비교 기준선입니다.
 */
@State(Scope.Thread)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 1, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(value = 1)
open class VersionedDictionaryBenchmark {

    private lateinit var baseDictionary: Map<KoreanPos, Set<String>>
    private lateinit var addedDictionary: Map<KoreanPos, Set<String>>

    @Setup(Level.Iteration)
    fun setup() {
        val current = KoreanDictionaryProvider.currentDictionarySnapshot()
        baseDictionary = current.value
        val nounWords = current.value[Noun].orEmpty()
        addedDictionary = current.value + (Noun to (nounWords + MUTATION_WORD))

        KoreanDictionaryProvider.reloadDictionaries(
            DictionaryVersion("korean-dictionary", current.version.revision + 1),
            baseDictionary,
        )
    }

    @TearDown(Level.Iteration)
    fun tearDown() {
        KoreanDictionaryProvider.reloadDictionaries(
            DictionaryVersion(
                "korean-dictionary",
                KoreanDictionaryProvider.currentDictionarySnapshot().version.revision + 1,
            ),
            baseDictionary,
        )
    }

    /** 실제 provider COW add/remove pair의 처리량입니다. */
    @Benchmark
    @OperationsPerInvocation(2)
    fun addRemoveWithCopyOnWrite(): Int {
        KoreanDictionaryProvider.addWordsToDictionary(Noun, listOf(MUTATION_WORD))
        KoreanDictionaryProvider.removeWordsFromDictionary(Noun, listOf(MUTATION_WORD))
        return KoreanDictionaryProvider.currentDictionarySnapshot().value[Noun]?.size ?: 0
    }

    /** 동일한 cardinality workload를 전체 replacement 경로로 수행한 처리량입니다. */
    @Benchmark
    @OperationsPerInvocation(2)
    fun addRemoveWithFullReplacement(): Int {
        val currentRevision = KoreanDictionaryProvider.currentDictionarySnapshot().version.revision
        KoreanDictionaryProvider.reloadDictionaries(
            DictionaryVersion("korean-dictionary", currentRevision + 1),
            addedDictionary,
        )
        KoreanDictionaryProvider.reloadDictionaries(
            DictionaryVersion("korean-dictionary", currentRevision + 2),
            baseDictionary,
        )
        return KoreanDictionaryProvider.currentDictionarySnapshot().value[Noun]?.size ?: 0
    }

    companion object {
        private const val MUTATION_WORD = "__issue239_benchmark_mutation__"
    }
}
