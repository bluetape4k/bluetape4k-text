package io.bluetape4k.tokenizer.exceptions

import io.bluetape4k.exceptions.BluetapeException

/**
 * Tokenizer module 전체에서 사용하는 base exception type입니다.
 *
 * ## 동작 계약
 * - Upstream exception-handling policy를 따르기 위해 `BluetapeException`을 확장합니다.
 * - 기본 no-arg constructor와 message/cause 조합 constructor를 모두 제공합니다.
 * - Module-specific exception은 더 세밀한 분류를 위해 이 type을 확장합니다.
 *
 * ```kotlin
 * val ex = TokenizerException("tokenize failed")
 * // ex.message == "tokenize failed"
 * ```
 */
open class TokenizerException: BluetapeException {
    /**
     * Message와 cause가 없는 exception을 만듭니다.
     *
     * ```kotlin
     * val ex = TokenizerException()
     * // ex.message == null
     * ```
     */
    constructor(): super()

    /**
     * 설명용 [message]를 담은 exception을 만듭니다.
     *
     * @param message 실패 원인을 설명하는 message입니다.
     *
     * ```kotlin
     * val ex = TokenizerException("invalid input")
     * // ex.message == "invalid input"
     * ```
     */
    constructor(message: String): super(message)

    /**
     * [message]와 [cause]를 함께 담은 exception을 만듭니다.
     *
     * ## 동작 계약
     * - [message]와 [cause]를 모두 parent constructor로 전달합니다.
     * - Exception-chain tracing이 필요할 때 사용합니다.
     *
     * @param message 실패 원인을 설명하는 message입니다.
     * @param cause 이 exception을 유발한 원본 throwable입니다.
     *
     * ```kotlin
     * val cause = IllegalArgumentException("invalid")
     * val ex = TokenizerException("request error", cause)
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
     * val cause = RuntimeException("boom")
     * val ex = TokenizerException(cause)
     * // ex.cause == cause
     * ```
     */
    constructor(cause: Throwable?): super(cause)
}
