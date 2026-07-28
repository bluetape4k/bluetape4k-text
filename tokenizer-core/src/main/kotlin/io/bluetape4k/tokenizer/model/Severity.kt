package io.bluetape4k.tokenizer.model

/**
 * 금칙어 노출 위험 수준을 나타내는 enum입니다.
 *
 * ## 동작 계약
 * - 위험도는 `LOW` < `MIDDLE` < `HIGH` 순서로 커집니다.
 * - 기본값은 [DEFAULT]로 공개하며 [LOW]에 매핑됩니다.
 * - Block-word filter option에서 감지 threshold를 지정할 때 사용합니다.
 *
 * ```kotlin
 * val severity = Severity.DEFAULT
 * // severity == Severity.LOW
 * ```
 */
enum class Severity {

    /** 낮은 severity입니다. 14세 미만 사용자에게 부적절한 slang과 colloquialism을 나타냅니다. */
    LOW,

    /** 중간 severity입니다. 19세 미만 미성년자에게 부적절한 profanity를 나타냅니다. */
    MIDDLE,

    /** 높은 severity입니다. 모든 연령에 부적절한 hate speech, regional slur, 극단적 profanity를 나타냅니다. */
    HIGH;

    companion object {
        /**
         * 기본 severity level(`LOW`)입니다.
         *
         * ## 동작 계약
         * - [BlockwordOptions]를 만들 때 기본값으로 사용합니다.
         * - Constant reference이며 runtime에 값이 바뀌지 않습니다.
         *
         * ```kotlin
         * val defaultSeverity = Severity.DEFAULT
         * // defaultSeverity == Severity.LOW
         * ```
         */
        val DEFAULT = Severity.LOW
    }
}
