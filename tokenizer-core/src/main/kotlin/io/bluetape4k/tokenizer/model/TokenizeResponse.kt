package io.bluetape4k.tokenizer.model

/**
 * Response model that returns the token list produced by morphological analysis.
 *
 * ## Behavior / Contract
 * - [text] holds the original input string used for analysis.
 * - [tokens] defaults to an empty list, indicating no tokens were extracted.
 * - Inherits [AbstractMessage], so a response creation timestamp is recorded automatically.
 *
 * ```kotlin
 * val response = tokenizeResponseOf("Kotlin coroutines", listOf("Kotlin", "coroutines"))
 * // response.tokens.size == 2
 * // response.text == "Kotlin coroutines"
 * ```
 */
data class TokenizeResponse(
    val text: String,
    val tokens: List<String> = emptyList(),
): AbstractMessage()

/**
 * Creates a [TokenizeResponse] with the given [text] and [tokens].
 *
 * ## Behavior / Contract
 * - Maps [text] and [tokens] directly into a new [TokenizeResponse].
 * - [tokens] defaults to an empty list when omitted.
 *
 * ```kotlin
 * val response = tokenizeResponseOf("sentence")
 * // response.tokens == emptyList<String>()
 * ```
 */
fun tokenizeResponseOf(
    text: String,
    tokens: List<String> = emptyList(),
): TokenizeResponse =
    TokenizeResponse(text, tokens)
