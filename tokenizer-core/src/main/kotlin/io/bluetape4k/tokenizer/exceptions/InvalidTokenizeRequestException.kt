package io.bluetape4k.tokenizer.exceptions

/**
 * Tokenize request의 input format이나 value가 유효하지 않을 때 던지는 exception입니다.
 *
 * ## 동작 계약
 * - Tokenizer exception hierarchy에 참여하도록 [TokenizerException]을 확장합니다.
 * - Validation-failure context를 전달하기 위해 message/cause constructor 조합을 제공합니다.
 * - Request-validation layer에서 domain exception을 분리할 때 사용합니다.
 *
 * ```kotlin
 * val ex = InvalidTokenizeRequestException("text must not be blank")
 * // ex is TokenizerException
 * ```
 */
open class InvalidTokenizeRequestException: TokenizerException {
    /**
     * Message와 cause가 없는 exception을 만듭니다.
     *
     * ```kotlin
     * val ex = InvalidTokenizeRequestException()
     * // ex.message == null
     * ```
     */
    constructor(): super()

    /**
     * Validation failure를 설명하는 [message]를 담은 exception을 만듭니다.
     *
     * @param message request validation 실패 원인을 설명하는 message입니다.
     *
     * ```kotlin
     * val ex = InvalidTokenizeRequestException("locale value is invalid")
     * // ex.message?.contains("locale") == true
     * ```
     */
    constructor(message: String): super(message)

    /**
     * [message]와 [cause]를 함께 담은 exception을 만듭니다.
     *
     * ## 동작 계약
     * - [message]와 [cause]를 모두 parent constructor로 전달합니다.
     * - 원본 exception을 chain 안에 보존합니다.
     *
     * @param message request validation 실패 원인을 설명하는 message입니다.
     * @param cause 이 exception을 유발한 원본 throwable입니다.
     *
     * ```kotlin
     * val cause = NumberFormatException("NaN")
     * val ex = InvalidTokenizeRequestException("token length parse failure", cause)
     * // ex.cause == cause
     * ```
     */
    constructor(message: String, cause: Throwable?): super(message, cause)

    /**
     * 추가 message 없이 [cause]를 wrapping하는 exception을 만듭니다.
     *
     * @param cause 이 exception을 유발한 원본 throwable입니다.
     *
     * ```kotlin
     * val cause = IllegalStateException("invalid request")
     * val ex = InvalidTokenizeRequestException(cause)
     * // ex.cause == cause
     * ```
     */
    constructor(cause: Throwable?): super(cause)
}
