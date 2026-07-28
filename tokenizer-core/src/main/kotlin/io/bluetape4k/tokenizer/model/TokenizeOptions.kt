package io.bluetape4k.tokenizer.model

import io.bluetape4k.logging.KLogging
import java.io.Serializable
import java.util.*

/**
 * 형태소 분석 중 locale 기반 동작을 제어하는 option입니다.
 *
 * ## 동작 계약
 * - 기본 locale은 `Locale.KOREAN`입니다.
 * - 현재 공개 field는 `locale` 하나이며, analyzer가 확장되면 option이 추가될 수 있습니다.
 * - Serializable request model 안에서 재사용할 수 있도록 [Serializable]을 구현합니다.
 *
 * @property locale 형태소 분석에 사용할 locale입니다. 기본값은 `Locale.KOREAN`입니다.
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
         * 기본 [TokenizeOptions] instance입니다.
         *
         * ## 동작 계약
         * - `locale=Locale.KOREAN`으로 초기화됩니다.
         * - 반복해서 사용할 수 있는 static default로 제공합니다.
         *
         * ```kotlin
         * val defaults = TokenizeOptions.DEFAULT
         * // defaults == TokenizeOptions()
         * ```
         */
        val DEFAULT = TokenizeOptions()
    }
}
