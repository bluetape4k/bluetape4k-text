package io.bluetape4k.tokenizer.model

import io.bluetape4k.support.requireNotBlank

/**
 * Maximum number of characters accepted by the blockword factory function.
 *
 * Callers that need a higher limit must construct [BlockwordRequest] directly and apply
 * their own validation. This guard exists to protect library consumers that expose
 * blockword APIs over HTTP without an upstream input-length gate.
 */
const val MAX_BLOCKWORD_TEXT_LENGTH: Int = 100_000

/**
 * Input model for requesting block-word detection and masking.
 *
 * ## Behavior / Contract
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
        text.requireNotBlank("text")
    }
}

/**
 * Creates a [BlockwordRequest] after validating [text].
 *
 * ## Behavior / Contract
 * - Calls `text.requireNotBlank("text")` before construction; blank input is rejected.
 * - Throws [IllegalArgumentException] when `text.length > MAX_BLOCKWORD_TEXT_LENGTH`.
 *
 * ## Input length
 * The factory rejects inputs longer than [MAX_BLOCKWORD_TEXT_LENGTH] characters.
 * Callers that need a higher limit must construct [BlockwordRequest] directly and
 * perform their own length validation before calling the blockword processors.
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
    text.requireNotBlank("text")
    require(text.length <= MAX_BLOCKWORD_TEXT_LENGTH) {
        "text too long: ${text.length} chars (max $MAX_BLOCKWORD_TEXT_LENGTH)"
    }
    return BlockwordRequest(text, options)
}
