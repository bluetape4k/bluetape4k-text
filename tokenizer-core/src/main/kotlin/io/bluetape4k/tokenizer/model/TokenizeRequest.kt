package io.bluetape4k.tokenizer.model

import io.bluetape4k.support.requireInRange
import io.bluetape4k.support.requireNotBlank

/**
 * [TokenizeRequest]가 허용하는 최대 문자 수입니다.
 *
 * 이 guard는 upstream input-length gate 없이 tokenize API를 HTTP로 노출하는 library consumer를
 * 보호하기 위해 둡니다.
 */
const val MAX_TOKENIZE_TEXT_LENGTH: Int = 100_000

/**
 * [textLength]가 tokenize input-length contract 안에 있는지 검증합니다.
 *
 * 예외 message에는 length만 보고하므로 원본 사용자 input이 log나 API error payload에
 * 노출되지 않습니다.
 *
 * @param textLength 검증할 입력 문자열의 문자 수입니다.
 */
fun requireTokenizeTextLength(textLength: Int) {
    textLength.requireInRange(0, MAX_TOKENIZE_TEXT_LENGTH) {
        "text too long: $textLength chars (max $MAX_TOKENIZE_TEXT_LENGTH)"
    }
}

/**
 * [text]가 tokenize input-length contract 안에 있는지 검증합니다.
 *
 * @param text 길이를 검증할 tokenize 입력 문자열입니다.
 */
fun requireTokenizeTextLength(text: CharSequence) {
    requireTokenizeTextLength(text.length)
}

/**
 * Tokenize할 text와 option을 함께 전달하는 request model입니다.
 *
 * ## 동작 계약
 * - 생성 시점에 `text.length > MAX_TOKENIZE_TEXT_LENGTH`를 거부합니다.
 * - 생성 시점에 `text.requireNotBlank("text")`를 검증하므로 blank input은 거부됩니다.
 * - [options]를 생략하면 [TokenizeOptions.DEFAULT]를 사용합니다.
 * - [AbstractMessage]를 상속하므로 creation timestamp가 자동으로 기록됩니다.
 *
 * @property text Tokenize할 원문 문자열입니다. Blank이면 안 되며 최대 길이를 넘을 수 없습니다.
 * @property options 형태소 분석 locale 등 tokenize 동작을 제어하는 option입니다.
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
    companion object {
        private const val serialVersionUID: Long = 1L
    }

    init {
        requireTokenizeTextLength(text)
        text.requireNotBlank("text")
    }
}

/**
 * [text]를 검증한 뒤 [TokenizeRequest]를 만듭니다.
 *
 * ## 동작 계약
 * - `text.length > MAX_TOKENIZE_TEXT_LENGTH`이면 [IllegalArgumentException]을 던집니다.
 * - 생성 전에 `text.requireNotBlank("text")`를 호출하므로 blank input은 거부됩니다.
 *
 * ## 입력 길이
 * 요청 model은 factory 호출, 직접 생성, JSON binding 모두에서
 * [MAX_TOKENIZE_TEXT_LENGTH]자를 넘는 input을 거부합니다.
 *
 * @param text Tokenize할 원문 문자열입니다.
 * @param options 형태소 분석 locale 등 tokenize 동작을 제어하는 option입니다.
 * @return 검증을 통과한 [TokenizeRequest]입니다.
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
