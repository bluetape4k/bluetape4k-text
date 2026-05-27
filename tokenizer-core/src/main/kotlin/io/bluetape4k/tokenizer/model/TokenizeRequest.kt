package io.bluetape4k.tokenizer.model

import io.bluetape4k.support.requireNotBlank

/**
 * Maximum number of characters accepted by [TokenizeRequest].
 *
 * This guard exists to protect library consumers that expose tokenize APIs over
 * HTTP without an upstream input-length gate.
 */
const val MAX_TOKENIZE_TEXT_LENGTH: Int = 100_000

/**
 * Validates that [textLength] is within the tokenize input-length contract.
 *
 * The exception message reports only lengths, so raw user input is not echoed
 * into logs or API error payloads.
 */
fun requireTokenizeTextLength(textLength: Int) {
    require(textLength <= MAX_TOKENIZE_TEXT_LENGTH) {
        "text too long: $textLength chars (max $MAX_TOKENIZE_TEXT_LENGTH)"
    }
}

/**
 * Validates that [text] is within the tokenize input-length contract.
 */
fun requireTokenizeTextLength(text: CharSequence) {
    requireTokenizeTextLength(text.length)
}

/**
 * Request model that carries the text to be tokenized along with its options.
 *
 * ## Behavior / Contract
 * - Rejects `text.length > MAX_TOKENIZE_TEXT_LENGTH` at construction.
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
        requireTokenizeTextLength(text)
        text.requireNotBlank("text")
    }
}

/**
 * Creates a [TokenizeRequest] after validating [text].
 *
 * ## Behavior / Contract
 * - Throws [IllegalArgumentException] when `text.length > MAX_TOKENIZE_TEXT_LENGTH`.
 * - Calls `text.requireNotBlank("text")` before construction; blank input is rejected.
 *
 * ## Input length
 * The request model rejects inputs longer than [MAX_TOKENIZE_TEXT_LENGTH]
 * characters across factory calls, direct construction, and JSON binding.
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
    requireTokenizeTextLength(text)
    text.requireNotBlank("text")
    return TokenizeRequest(text, options)
}
