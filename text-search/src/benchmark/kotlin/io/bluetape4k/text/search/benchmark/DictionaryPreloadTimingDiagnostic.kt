package io.bluetape4k.text.search.benchmark

import io.bluetape4k.tokenizer.japanese.JapaneseProcessor
import io.bluetape4k.tokenizer.korean.KoreanProcessor
import kotlinx.coroutines.runBlocking
import kotlin.system.measureTimeMillis

/**
 * 한국어·일본어 dictionary preload의 cold/warm 관측을 fresh JVM에서 수행합니다.
 *
 * 이 diagnostic은 JUnit suite의 process-wide singleton 상태와 분리된
 * `JavaExec` 프로세스에서 실행해야 합니다. 첫 번째 호출은 해당 provider의
 * cold preload, 두 번째 호출은 같은 프로세스의 warm preload를 측정합니다.
 * 결과는 성능 회귀 임계값이 아닌 재현 가능한 관측 자료입니다.
 */
object DictionaryPreloadTimingDiagnostic {

    @JvmStatic
    fun main(args: Array<String>) = runBlocking {
        val preloads = listOf(
            "korean" to suspend { KoreanProcessor.preload() },
            "japanese" to suspend { JapaneseProcessor.preload() },
        )

        preloads.forEach { (provider, preload) ->
            val coldMillis = measureTimeMillis { preload() }
            val warmMillis = measureTimeMillis { preload() }

            check(coldMillis >= 0) { "cold preload duration must be non-negative: $provider" }
            check(warmMillis >= 0) { "warm preload duration must be non-negative: $provider" }

            // 표준 출력은 이 diagnostic의 machine-readable raw evidence 경계다.
            println("ISSUE262_PRELOAD_TIMING provider=$provider coldMs=$coldMillis warmMs=$warmMillis")
        }
    }
}
