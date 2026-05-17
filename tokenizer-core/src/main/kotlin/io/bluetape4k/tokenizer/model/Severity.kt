package io.bluetape4k.tokenizer.model

/**
 * Enumeration representing the exposure risk level of a block word.
 *
 * ## Behavior / Contract
 * - Risk increases in order: `LOW` < `MIDDLE` < `HIGH`.
 * - The default value is exposed as [DEFAULT], which maps to [LOW].
 * - Used in block-word filter options to specify the detection threshold.
 *
 * ```kotlin
 * val severity = Severity.DEFAULT
 * // severity == Severity.LOW
 * ```
 */
enum class Severity {

    /** Low severity — slang and colloquialisms inappropriate for users under 14. */
    LOW,

    /** Medium severity — profanity inappropriate for minors under 19. */
    MIDDLE,

    /** High severity — hate speech, regional slurs, and extreme profanity inappropriate for all ages. */
    HIGH;

    companion object {
        /**
         * The default severity level (`LOW`).
         *
         * ## Behavior / Contract
         * - Used as the default when constructing [BlockwordOptions].
         * - A constant reference; its value never changes at runtime.
         *
         * ```kotlin
         * val defaultSeverity = Severity.DEFAULT
         * // defaultSeverity == Severity.LOW
         * ```
         */
        val DEFAULT = Severity.LOW
    }
}
