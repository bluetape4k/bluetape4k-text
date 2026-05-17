package io.bluetape4k.tokenizer.model

import io.bluetape4k.logging.KLogging
import java.io.Serializable
import java.util.*

/**
 * Request options that control block-word masking and detection severity.
 *
 * ## Behavior / Contract
 * - Default mask string is `"*"`; callers may substitute any replacement string.
 * - Default locale is `Locale.KOREAN`.
 * - Default severity is `Severity.DEFAULT` (`LOW`).
 *
 * ```kotlin
 * val options = blockwordOptionsOf(mask = "#", severity = Severity.HIGH)
 * // options.mask == "#"
 * // options.locale == Locale.KOREAN
 * // options.severity == Severity.HIGH
 * ```
 */
data class BlockwordOptions(
    val mask: String = "*",
    val locale: Locale = Locale.KOREAN,
    val severity: Severity = Severity.DEFAULT,
): Serializable {
    companion object : KLogging() {
        private const val serialVersionUID = 1L

        /**
         * The default [BlockwordOptions] instance.
         *
         * ## Behavior / Contract
         * - Uses `mask="*"`, `locale=Locale.KOREAN`, `severity=Severity.DEFAULT`.
         * - Provided as a reusable static default value.
         *
         * ```kotlin
         * val defaults = BlockwordOptions.DEFAULT
         * // defaults.mask == "*"
         * // defaults.severity == Severity.LOW
         * ```
         */
        val DEFAULT = BlockwordOptions()
    }
}

/**
 * Creates a [BlockwordOptions] instance with the given parameters.
 *
 * ## Behavior / Contract
 * - Maps [mask], [locale], and [severity] directly into a new [BlockwordOptions].
 * - Omitting arguments applies the same defaults as [BlockwordOptions.DEFAULT].
 *
 * ```kotlin
 * val options = blockwordOptionsOf()
 * // options == BlockwordOptions.DEFAULT
 * ```
 */
fun blockwordOptionsOf(
    mask: String = "*",
    locale: Locale = Locale.KOREAN,
    severity: Severity = Severity.DEFAULT,
): BlockwordOptions = BlockwordOptions(mask, locale, severity)
