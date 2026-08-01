@file:Suppress("MatchingDeclarationName")

package io.bluetape4k.text.examples.tokenizer

import com.github.pemistahl.lingua.api.Language
import io.bluetape4k.lingua.detectLanguageSegments
import io.bluetape4k.lingua.languageDetectorOf
import io.bluetape4k.text.search.AhoCorasickAutomaton
import io.bluetape4k.text.search.AhoCorasickMatch
import io.bluetape4k.text.search.SearchOptions
import io.bluetape4k.tokenizer.japanese.JapaneseProcessor
import io.bluetape4k.tokenizer.korean.KoreanProcessor
import io.bluetape4k.tokenizer.model.MAX_TOKENIZE_TEXT_LENGTH
import java.io.Serial
import java.io.Serializable

private const val STATUS_OK = 200
private const val STATUS_BAD_REQUEST = 400
private const val STATUS_UNPROCESSABLE_ENTITY = 422
private const val STATUS_REQUEST_ENTITY_TOO_LARGE = 413
private const val STATUS_INTERNAL_SERVER_ERROR = 500

/** Aho-Corasick 매치가 keyword인지 blockword인지 나타냅니다. */
internal enum class ModerationMatchKind {
    KEYWORD,
    BLOCKWORD,
}

/** moderation 응답에 포함할 매치 요약입니다. */
internal data class ModerationMatch(
    /** 매치의 정책 분류입니다. */
    val kind: ModerationMatchKind,
    /** 원문에서 탐지한 keyword입니다. */
    val keyword: String,
    /** UTF-16 기준 inclusive 시작 offset입니다. */
    val start: Int,
    /** UTF-16 기준 inclusive 종료 offset입니다. */
    val end: Int,
) : Serializable {
    companion object {
        @Serial
        private const val serialVersionUID: Long = 1L
    }
}

/** 입력 언어, 토큰 수, 매치와 마스킹 결과를 함께 반환하는 moderation 응답입니다. */
internal data class TextModerationResponse(
    /** HTTP adapter가 사용할 상태 코드입니다. */
    val statusCode: Int,
    /** 입력에서 감지한 지원 언어 집합입니다. */
    val detectedLanguages: Set<Language>,
    /** 한국어·일본어 facade와 영문 fallback으로 계산한 토큰 수입니다. */
    val tokenCount: Int,
    /** keyword와 blockword 매치 요약입니다. */
    val matches: List<ModerationMatch>,
    /** 탐지된 구간을 동일 길이의 `*`로 치환한 텍스트입니다. */
    val maskedText: String,
    /** 실패 시 원문을 포함하지 않는 안전한 오류 메시지입니다. */
    val error: String? = null,
) : Serializable {
    companion object {
        @Serial
        private const val serialVersionUID: Long = 1L
    }
}

/**
 * 공개 tokenizer, Lingua, Aho-Corasick API를 조합한 결정적 moderation 서비스 예제입니다.
 *
 * 실제 HTTP adapter는 [moderate]의 응답을 상태 코드와 JSON으로 변환하면 됩니다. 서비스는
 * detector와 automaton을 재사용하고, 요청마다 원문을 오류 메시지에 넣지 않습니다.
 */
internal class TextModerationService {

    private val detector = languageDetectorOf(
        languages = setOf(Language.ENGLISH, Language.KOREAN, Language.JAPANESE),
        minimumRelativeDistance = 0.0,
        isEveryLanguageModelPreloaded = true,
    )

    private val moderationAutomaton: AhoCorasickAutomaton<ModerationMatchKind> =
        AhoCorasickAutomaton.builder<ModerationMatchKind>()
            .add("password", ModerationMatchKind.KEYWORD)
            .add("secret", ModerationMatchKind.KEYWORD)
            .add("badword", ModerationMatchKind.BLOCKWORD)
            .add("비속어", ModerationMatchKind.BLOCKWORD)
            .options(SearchOptions(ignoreCase = true, allowOverlaps = false))
            .build()

    /**
     * 입력을 검증하고 언어별 tokenizer, keyword/blockword 검색 결과를 하나로 묶습니다.
     *
     * @param text moderation할 입력 문자열입니다.
     * @return 성공, 입력 거부, 처리 실패를 구분한 안전한 응답입니다.
     */
    fun moderate(text: String): TextModerationResponse =
        validate(text) ?: runCatching {
            val segments = detector.detectLanguageSegments(text)
            val languages = segments.mapTo(linkedSetOf()) { it.language }
            if (languages.isEmpty()) {
                rejected(STATUS_UNPROCESSABLE_ENTITY, "supported language was not detected")
            } else {
                val tokenCount = segments.sumOf { segment ->
                    val segmentText = text.substring(segment.start, segment.endExclusive)
                    when (segment.language) {
                        Language.KOREAN -> KoreanProcessor.tokenize(segmentText).size
                        Language.JAPANESE -> JapaneseProcessor.tokenize(segmentText).size
                        else -> segmentText.split(Regex("\\s+")).count(String::isNotBlank)
                    }
                }
                val matches = moderationAutomaton.parseText(text).map(::toModerationMatch)
                val maskedText = moderationAutomaton.replaceAll(text) { match -> "*".repeat(match.length) }
                TextModerationResponse(STATUS_OK, languages, tokenCount, matches, maskedText)
            }
        }.getOrElse {
            rejected(STATUS_INTERNAL_SERVER_ERROR, "moderation failed: processor error")
        }

    private fun validate(text: String): TextModerationResponse? = when {
        text.isBlank() -> rejected(STATUS_BAD_REQUEST, "text is blank")
        text.length > MAX_TOKENIZE_TEXT_LENGTH ->
            rejected(STATUS_REQUEST_ENTITY_TOO_LARGE, "text is too long: ${text.length} chars")
        else -> null
    }

    private fun rejected(statusCode: Int, error: String): TextModerationResponse =
        TextModerationResponse(statusCode, emptySet(), 0, emptyList(), "", error)

    private fun toModerationMatch(match: AhoCorasickMatch<ModerationMatchKind>): ModerationMatch =
        ModerationMatch(match.value, match.keyword, match.start, match.end)
}

/** moderation 응답을 CLI smoke test에 사용할 수 있는 문자열로 렌더링합니다. */
internal fun renderTextModerationResponse(response: TextModerationResponse): String = buildString {
    appendLine("status=${response.statusCode}")
    appendLine("languages=${response.detectedLanguages.joinToString()}")
    appendLine("tokens=${response.tokenCount}")
    appendLine("matches=${response.matches.joinToString { "${it.kind}:${it.keyword}" }}")
    appendLine("masked=${response.maskedText}")
    response.error?.let { appendLine("error=$it") }
}

internal fun runTextModerationExample(): TextModerationResponse =
    TextModerationService().moderate("안녕하세요 password와 비속어를 검사합니다")
