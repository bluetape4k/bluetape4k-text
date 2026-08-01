package io.bluetape4k.lingua

import com.github.pemistahl.lingua.api.Language

/**
 * 입력 문자열에서 하나의 언어로 판정된 연속 문자 구간입니다.
 *
 * `start`와 `endExclusive`는 Kotlin `String`의 UTF-16 인덱스입니다. 구두점, 공백,
 * 이모지는 구간에 포함하지 않으며, 같은 언어의 인접 구간만 하나로 합칩니다.
 *
 * @property start 원문에서 구간이 시작하는 UTF-16 인덱스입니다.
 * @property endExclusive 원문에서 구간이 끝나는 다음 UTF-16 인덱스입니다.
 * @property language 구간에서 감지한 언어입니다. 신뢰도가 낮아도 요청하면 `UNKNOWN`을 반환할 수 있습니다.
 * @property confidence 감지 결과의 신뢰도입니다. 범위는 `0.0..1.0`입니다.
 */
data class LanguageSegment(
    val start: Int,
    val endExclusive: Int,
    val language: Language,
    val confidence: Double,
) {
    /** 구간의 UTF-16 문자 길이입니다. */
    val length: Int get() = endExclusive - start
}
