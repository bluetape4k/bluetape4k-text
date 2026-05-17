package io.bluetape4k.tokenizer.model

/**
 * Response model that holds block-word processing results and the list of detected words.
 *
 * ## Behavior / Contract
 * - [maskedText] stores the final masked string as provided by the caller.
 * - [blockWords] defaults to an empty list, indicating no words were detected.
 * - [blockwordExists] is a computed property derived from `blockWords.isNotEmpty()`.
 *
 * ```kotlin
 * val request = blockwordRequestOf("sentence")
 * val response = blockwordResponseOf(request, "sentence", listOf("badword"))
 * // response.blockwordExists == true
 * // response.blockWords.size == 1
 * ```
 */
data class BlockwordResponse(
    val request: BlockwordRequest,
    val maskedText: String,
    val blockWords: List<String> = emptyList(),
): AbstractMessage() {
    /**
     * Returns `true` when at least one block word was detected.
     *
     * ## Behavior / Contract
     * - Returns `true` when [blockWords] is not empty.
     * - Computed property; no separate state is stored.
     *
     * ```kotlin
     * val empty = blockwordResponseOf(blockwordRequestOf("sentence"), "sentence")
     * // empty.blockwordExists == false
     * ```
     */
    val blockwordExists: Boolean
        get() = blockWords.isNotEmpty()
}

/**
 * Creates a [BlockwordResponse] with the given [request], [maskedText], and [blockWords].
 *
 * ## Behavior / Contract
 * - Maps all parameters directly into a new [BlockwordResponse].
 * - [blockWords] defaults to an empty list when omitted.
 *
 * ```kotlin
 * val request = blockwordRequestOf("sentence")
 * val response = blockwordResponseOf(request, "***")
 * // response.maskedText == "***"
 * // response.blockwordExists == false
 * ```
 */
fun blockwordResponseOf(
    request: BlockwordRequest,
    maskedText: String,
    blockWords: List<String> = emptyList(),
): BlockwordResponse = BlockwordResponse(request, maskedText, blockWords)
