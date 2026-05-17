package io.bluetape4k.tokenizer.model

import io.bluetape4k.support.requireNotBlank

/**
 * Maximum number of characters accepted by the tokenize factory function.
 *
 * Callers that need a higher limit must construct [TokenizeRequest] directly and apply
 * their own validation. This guard exists to protect library consumers that expose
 * tokenize APIs over HTTP without an upstream input-length gate.
 */
const val MAX_TOKENIZE_TEXT_LENGTH: Int = 100_000

/**
 * Request model that carries the text to be tokenized along with its options.
 *
 * ## Behavior / Contract
 * - Validates `text.requireNotBlank("text")` at construction; blank input is rejected.
 * - When [options] is omitted, [TokenizeOptions.DEFAULT] is used.
 * - Inherits [AbstractMessage], so a creation timestamp is recorded automatically.
 *
 * ```kotlin
 * val request = tokenizeRequestOf("Kotlin coroutines")
 * // request.text == "Kotlin coroutines"
 * // request.options == TokenizeOptions.DEFAULT
 * ```
 */
data class TokenizeRequest(
    val text: String,
    val options: TokenizeOptions = TokenizeOptions.DEFAULT,
): AbstractMessage() {
    init {
        text.requireNotBlank("text")
    }
}

/**
 * Creates a [TokenizeRequest] after validating [text].
 *
 * ## Behavior / Contract
 * - Calls `text.requireNotBlank("text")` before construction; blank input is rejected.
 * - Throws [IllegalArgumentException] when `text.length > MAX_TOKENIZE_TEXT_LENGTH`.
 *
 * ## Input length
 * The factory rejects inputs longer than [MAX_TOKENIZE_TEXT_LENGTH] characters.
 * Callers that need a higher limit must construct [TokenizeRequest] directly and
 * perform their own length validation before calling the tokenize processors.
 *
 * ```kotlin
 * val request = tokenizeRequestOf("async processing", TokenizeOptions())
 * // request.text == "async processing"
 * // request.options.locale == Locale.KOREAN
 * ```
 */
fun tokenizeRequestOf(
    text: String,
    options: TokenizeOptions = TokenizeOptions.DEFAULT,
): TokenizeRequest {
    text.requireNotBlank("text")
    require(text.length <= MAX_TOKENIZE_TEXT_LENGTH) {
        "text too long: ${text.length} chars (max $MAX_TOKENIZE_TEXT_LENGTH)"
    }
    return TokenizeRequest(text, options)
}
