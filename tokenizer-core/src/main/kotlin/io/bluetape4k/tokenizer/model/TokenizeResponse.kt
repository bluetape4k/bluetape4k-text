package io.bluetape4k.tokenizer.model

/**
 * 형태소 분석이 만든 token list를 반환하는 response model입니다.
 *
 * ## 동작 계약
 * - [text]는 분석에 사용한 원문 input string을 보관합니다.
 * - [tokens]의 기본값은 empty list이며 추출한 token이 없음을 뜻합니다.
 * - [AbstractMessage]를 상속하므로 response creation timestamp가 자동으로 기록됩니다.
 *
 * @property text 분석에 사용한 원문 문자열입니다.
 * @property tokens 형태소 분석 결과 token 목록입니다. 추출 결과가 없으면 empty list입니다.
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
 * 전달한 [text]와 [tokens]로 [TokenizeResponse]를 만듭니다.
 *
 * ## 동작 계약
 * - [text]와 [tokens]를 새 [TokenizeResponse]에 그대로 매핑합니다.
 * - [tokens]를 생략하면 empty list를 사용합니다.
 *
 * @param text 분석에 사용한 원문 문자열입니다.
 * @param tokens 형태소 분석 결과 token 목록입니다.
 * @return 전달한 결과 값을 담은 [TokenizeResponse]입니다.
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
