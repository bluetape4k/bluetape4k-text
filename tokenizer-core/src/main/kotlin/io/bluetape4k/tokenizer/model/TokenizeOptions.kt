package io.bluetape4k.tokenizer.model

import io.bluetape4k.logging.KLogging
import java.io.Serializable
import java.util.*

/**
 * Options that control locale-based behaviour during morphological analysis.
 *
 * ## Behavior / Contract
 * - Default locale is `Locale.KOREAN`.
 * - Currently exposes one public field (`locale`); additional options may be added as analyzers evolve.
 * - Implements [Serializable] for reuse inside serializable request models.
 *
 * ```kotlin
 * val options = TokenizeOptions.DEFAULT
 * // options.locale == Locale.KOREAN
 * ```
 */
data class TokenizeOptions(
    val locale: Locale = Locale.KOREAN,
): Serializable {
    companion object : KLogging() {
        private const val serialVersionUID = 1L

        /**
         * The default [TokenizeOptions] instance.
         *
         * ## Behavior / Contract
         * - Initialized with `locale=Locale.KOREAN`.
         * - Provided as a reusable static default.
         *
         * ```kotlin
         * val defaults = TokenizeOptions.DEFAULT
         * // defaults == TokenizeOptions()
         * ```
         */
        val DEFAULT = TokenizeOptions()
    }
}
