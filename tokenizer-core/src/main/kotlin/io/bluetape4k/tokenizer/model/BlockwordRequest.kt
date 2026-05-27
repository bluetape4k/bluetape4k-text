package io.bluetape4k.tokenizer.model

import io.bluetape4k.support.requireNotBlank

/**
 * Maximum number of characters accepted by [BlockwordRequest].
 *
 * This guard exists to protect library consumers that expose blockword APIs over
 * HTTP without an upstream input-length gate.
 */
const val MAX_BLOCKWORD_TEXT_LENGTH: Int = 100_000

/**
 * Validates that [text] is within the blockword input-length contract.
 *
 * The exception message reports only lengths, so raw user input is not echoed
 * into logs or API error payloads.
 */
fun requireBlockwordTextLength(text: CharSequence) {
    require(text.length <= MAX_BLOCKWORD_TEXT_LENGTH) {
        "text too long: ${text.length} chars (max $MAX_BLOCKWORD_TEXT_LENGTH)"
    }
}

/**
 * Input model for requesting block-word detection and masking.
 *
 * ## Behavior / Contract
 * - Rejects `text.length > MAX_BLOCKWORD_TEXT_LENGTH` at construction time.
 * - Validates `text.requireNotBlank("text")` at construction time; blank input is rejected.
 * - When [options] is omitted, [BlockwordOptions.DEFAULT] is used.
 * - Inherits [AbstractMessage], so a creation timestamp is recorded automatically.
 *
 * ```kotlin
 * val request = blockwordRequestOf("bad word", blockwordOptionsOf(mask = "*"))
 * // request.text == "bad word"
 * // request.options.mask == "*"
 * ```
 */
data class BlockwordRequest(
    val text: String,
    val options: BlockwordOptions = BlockwordOptions.DEFAULT,
): AbstractMessage() {
    init {
        requireBlockwordTextLength(text)
        text.requireNotBlank("text")
    }
}

/**
 * Creates a [BlockwordRequest] after validating [text].
 *
 * ## Behavior / Contract
 * - Throws [IllegalArgumentException] when `text.length > MAX_BLOCKWORD_TEXT_LENGTH`.
 * - Calls `text.requireNotBlank("text")` before construction; blank input is rejected.
 *
 * ## Input length
 * The request model rejects inputs longer than [MAX_BLOCKWORD_TEXT_LENGTH]
 * characters across factory calls, direct construction, and JSON binding.
 *
 * ```kotlin
 * val request = blockwordRequestOf("test sentence")
 * // request.text == "test sentence"
 * // request.options == BlockwordOptions.DEFAULT
 * ```
 */
fun blockwordRequestOf(
    text: String,
    options: BlockwordOptions = BlockwordOptions.DEFAULT,
): BlockwordRequest {
    requireBlockwordTextLength(text)
    text.requireNotBlank("text")
    return BlockwordRequest(text, options)
}
