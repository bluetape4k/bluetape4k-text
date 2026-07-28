package io.bluetape4k.tokenizer.model

import io.bluetape4k.logging.KLogging
import java.io.Serializable
import java.util.*

/**
 * 금칙어 masking과 감지 severity를 제어하는 request option입니다.
 *
 * ## 동작 계약
 * - 기본 mask 문자열은 `"*"`이며, 호출자는 원하는 replacement string으로 바꿀 수 있습니다.
 * - 기본 locale은 `Locale.KOREAN`입니다.
 * - 기본 severity는 `Severity.DEFAULT`(`LOW`)입니다.
 *
 * @property mask 감지한 금칙어를 대체할 문자열입니다. 기본값은 `"*"`입니다.
 * @property locale 금칙어 처리에 사용할 locale입니다. 기본값은 `Locale.KOREAN`입니다.
 * @property severity 금칙어 감지 기준의 심각도입니다. 기본값은 [Severity.DEFAULT]입니다.
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
         * 기본 [BlockwordOptions] instance입니다.
         *
         * ## 동작 계약
         * - `mask="*"`, `locale=Locale.KOREAN`, `severity=Severity.DEFAULT`를 사용합니다.
         * - 반복해서 사용할 수 있는 static default value로 제공합니다.
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
 * 전달한 값으로 [BlockwordOptions] instance를 만듭니다.
 *
 * ## 동작 계약
 * - [mask], [locale], [severity]를 새 [BlockwordOptions]에 그대로 매핑합니다.
 * - 인자를 생략하면 [BlockwordOptions.DEFAULT]와 같은 기본값을 적용합니다.
 *
 * @param mask 감지한 금칙어를 대체할 문자열입니다. 생략하면 `"*"`를 사용합니다.
 * @param locale 금칙어 처리에 사용할 locale입니다. 생략하면 `Locale.KOREAN`을 사용합니다.
 * @param severity 금칙어 감지 기준의 심각도입니다. 생략하면 [Severity.DEFAULT]를 사용합니다.
 * @return 전달한 option 값을 담은 새 [BlockwordOptions]입니다.
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
