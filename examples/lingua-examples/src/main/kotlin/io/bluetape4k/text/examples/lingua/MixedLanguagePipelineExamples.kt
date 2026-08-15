@file:Suppress("MatchingDeclarationName")

package io.bluetape4k.text.examples.lingua

import com.github.pemistahl.lingua.api.Language
import com.github.pemistahl.lingua.api.LanguageDetector
import io.bluetape4k.lingua.LanguageSegment
import io.bluetape4k.lingua.detectLanguageSegments
import io.bluetape4k.lingua.languageDetectorOf
import io.bluetape4k.tokenizer.japanese.JapaneseProcessor
import io.bluetape4k.tokenizer.korean.KoreanProcessor

/** 혼합 언어 입력을 감지한 뒤 언어별 토크나이저로 라우팅하는 결과입니다. */
data class MixedLanguagePipelineResult(
    val source: String,
    val segments: List<LanguageSegment>,
    val tokenCounts: Map<Language, Int>,
)

/**
 * 혼합 언어 pipeline에서 재사용할 Lingua detector를 생성합니다.
 *
 * @param preloadModels 모든 언어 모델을 미리 읽을지 여부입니다. `true`는 첫 감지 지연을 줄이는 대신
 * 시작 비용과 메모리 사용량이 늘고, `false`는 초기 비용을 줄이는 대신 첫 감지 시 모델을 읽습니다.
 */
fun createMixedLanguageDetector(preloadModels: Boolean = true): LanguageDetector =
    languageDetectorOf(
        languages = setOf(Language.ENGLISH, Language.KOREAN, Language.JAPANESE),
        minimumRelativeDistance = 0.0,
        isEveryLanguageModelPreloaded = preloadModels,
    )

/**
 * 공개 Lingua·토크나이저 API만 사용해 혼합 언어 입력을 처리합니다.
 *
 * 영어와 알 수 없는 구간은 감지만 수행하고, 한국어·일본어 구간만 해당 facade로 전달합니다.
 * [detector]는 여러 입력에서 재사용해야 하며, 호출마다 새 detector를 만들지 않습니다.
 */
fun runMixedLanguagePipeline(
    text: String,
    detector: LanguageDetector,
    minimumConfidence: Double = 0.55,
): MixedLanguagePipelineResult {
    val segments = detector.detectLanguageSegments(text, minimumConfidence)
    val tokenCounts = segments
        .filter { it.language == Language.KOREAN || it.language == Language.JAPANESE }
        .groupingBy { it.language }
        .fold(0) { count, segment ->
            val segmentText = text.substring(segment.start, segment.endExclusive)
            count + when (segment.language) {
                Language.KOREAN -> KoreanProcessor.tokenize(segmentText).size
                Language.JAPANESE -> JapaneseProcessor.tokenize(segmentText).size
                else -> 0
            }
        }

    return MixedLanguagePipelineResult(text, segments, tokenCounts)
}

fun renderMixedLanguagePipeline(result: MixedLanguagePipelineResult): String = buildString {
    result.segments.forEach { segment ->
        appendLine(
            "${segment.language} ${segment.start}..${segment.endExclusive}: " +
                result.source.substring(segment.start, segment.endExclusive)
        )
    }
    result.tokenCounts.forEach { (language, count) -> appendLine("tokens[$language]=$count") }
}
