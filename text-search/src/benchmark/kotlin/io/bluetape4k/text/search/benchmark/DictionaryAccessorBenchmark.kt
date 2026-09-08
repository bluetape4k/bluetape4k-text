package io.bluetape4k.text.search.benchmark

import io.bluetape4k.tokenizer.japanese.utils.JapaneseDictionaryProvider
import io.bluetape4k.tokenizer.korean.utils.KoreanDictionaryProvider
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Noun
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.Setup
import org.openjdk.jmh.annotations.State

/** 초기 로딩 이후 공개 사전 조회와 불변 원본 직접 조회의 처리량을 비교합니다. */
@State(Scope.Thread)
open class DictionaryAccessorBenchmark {

    @Setup
    fun setup() {
        koreanDictionary()
        koreanBlockwords()
        koreanProperNouns()
        japaneseBlockwords()
    }

    @Benchmark
    fun koreanDictionary(): Boolean = KoreanDictionaryProvider.koreanDictionary.getValue(Noun).contains("한국")

    @Benchmark
    fun koreanBlockwords(): Int = KoreanDictionaryProvider.blockWords.values.sumOf { it.size }

    @Benchmark
    fun koreanProperNouns(): Boolean = KoreanDictionaryProvider.properNouns.contains("서울")

    @Benchmark
    fun japaneseBlockwords(): Boolean = JapaneseDictionaryProvider.blockWordDictionary.contains("馬鹿")

    @Benchmark
    fun koreanSnapshot(): Boolean = KoreanDictionaryProvider.currentDictionarySnapshot().value.getValue(Noun).contains("한국")

    @Benchmark
    fun japaneseSnapshot(): Boolean = JapaneseDictionaryProvider.currentBlockwordSnapshot().value.contains("馬鹿")
}
