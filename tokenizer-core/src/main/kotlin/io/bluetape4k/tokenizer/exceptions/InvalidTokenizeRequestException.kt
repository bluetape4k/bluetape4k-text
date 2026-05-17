package io.bluetape4k.tokenizer.exceptions

/**
 * Exception thrown when the input format or value of a tokenize request is invalid.
 *
 * ## Behavior / Contract
 * - Extends [TokenizerException] to participate in the tokenizer exception hierarchy.
 * - Provides message/cause constructor combinations to convey validation-failure context.
 * - Use this type to separate domain exceptions in the request-validation layer.
 *
 * ```kotlin
 * val ex = InvalidTokenizeRequestException("text must not be blank")
 * // ex is TokenizerException
 * ```
 */
open class InvalidTokenizeRequestException: TokenizerException {
    /**
     * Creates an exception with no message or cause.
     *
     * ```kotlin
     * val ex = InvalidTokenizeRequestException()
     * // ex.message == null
     * ```
     */
    constructor(): super()

    /**
     * Creates an exception with a validation-failure [message].
     *
     * ```kotlin
     * val ex = InvalidTokenizeRequestException("locale value is invalid")
     * // ex.message?.contains("locale") == true
     * ```
     */
    constructor(message: String): super(message)

    /**
     * Creates an exception with a [message] and a [cause].
     *
     * ## Behavior / Contract
     * - Passes both [message] and [cause] to the parent constructor.
     * - Preserves the originating exception in the chain.
     *
     * ```kotlin
     * val cause = NumberFormatException("NaN")
     * val ex = InvalidTokenizeRequestException("token length parse failure", cause)
     * // ex.cause == cause
     * ```
     */
    constructor(message: String, cause: Throwable?): super(message, cause)

    /**
     * Creates an exception wrapping [cause] with no additional message.
     *
     * ```kotlin
     * val cause = IllegalStateException("invalid request")
     * val ex = InvalidTokenizeRequestException(cause)
     * // ex.cause == cause
     * ```
     */
    constructor(cause: Throwable?): super(cause)
}
