package io.bluetape4k.tokenizer.exceptions

import io.bluetape4k.exceptions.BluetapeException

/**
 * Base exception type used throughout the tokenizer module.
 *
 * ## Behavior / Contract
 * - Extends `BluetapeException` to follow the upstream exception-handling policy.
 * - Provides a default no-arg constructor and all message/cause combinations.
 * - Module-specific exceptions extend this type for finer-grained classification.
 *
 * ```kotlin
 * val ex = TokenizerException("tokenize failed")
 * // ex.message == "tokenize failed"
 * ```
 */
open class TokenizerException: BluetapeException {
    /**
     * Creates an exception with no message or cause.
     *
     * ```kotlin
     * val ex = TokenizerException()
     * // ex.message == null
     * ```
     */
    constructor(): super()

    /**
     * Creates an exception with a descriptive [message].
     *
     * ```kotlin
     * val ex = TokenizerException("invalid input")
     * // ex.message == "invalid input"
     * ```
     */
    constructor(message: String): super(message)

    /**
     * Creates an exception with a [message] and a [cause].
     *
     * ## Behavior / Contract
     * - Both [message] and [cause] are forwarded to the parent constructor.
     * - Use for exception-chain tracing.
     *
     * ```kotlin
     * val cause = IllegalArgumentException("invalid")
     * val ex = TokenizerException("request error", cause)
     * // ex.cause == cause
     * ```
     */
    constructor(message: String, cause: Throwable?): super(message, cause)

    /**
     * Creates an exception wrapping [cause] with no additional message.
     *
     * ```kotlin
     * val cause = RuntimeException("boom")
     * val ex = TokenizerException(cause)
     * // ex.cause == cause
     * ```
     */
    constructor(cause: Throwable?): super(cause)
}
