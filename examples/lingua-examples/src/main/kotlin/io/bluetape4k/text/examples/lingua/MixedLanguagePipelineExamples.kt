@file:Suppress("MatchingDeclarationName")

package io.bluetape4k.text.examples.lingua

import com.github.pemistahl.lingua.api.Language
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
 * 공개 Lingua·토크나이저 API만 사용해 혼합 언어 입력을 처리합니다.
 *
 * 영어와 알 수 없는 구간은 감지만 수행하고, 한국어·일본어 구간만 해당 facade로 전달합니다.
 */
fun runMixedLanguagePipeline(
    text: String,
    minimumConfidence: Double = 0.55,
): MixedLanguagePipelineResult {
    val detector = languageDetectorOf(
        languages = setOf(Language.ENGLISH, Language.KOREAN, Language.JAPANESE),
        minimumRelativeDistance = 0.0,
        isEveryLanguageModelPreloaded = true,
    )
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
