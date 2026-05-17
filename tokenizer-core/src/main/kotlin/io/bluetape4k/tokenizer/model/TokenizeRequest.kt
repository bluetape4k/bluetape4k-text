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
 * 형태소 분석 대상 텍스트와 옵션을 전달하는 요청 모델이다.
 *
 * ## 동작/계약
 * - 생성 시 `text.requireNotBlank("text")`를 수행해 공백 입력을 거부한다.
 * - `options`를 생략하면 `TokenizeOptions.DEFAULT`를 사용한다.
 * - `AbstractMessage`의 `timestamp`가 함께 생성되어 요청 시각을 기록한다.
 *
 * ```kotlin
 * val request = tokenizeRequestOf("코틀린 코루틴")
 * // request.text == "코틀린 코루틴"
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
 * 형태소 분석 요청 객체를 생성한다.
 *
 * ## 동작/계약
 * - `text.requireNotBlank("text")` 검증 후 `TokenizeRequest`를 생성한다.
 * - `text.length > MAX_TOKENIZE_TEXT_LENGTH`이면 `IllegalArgumentException`을 던진다.
 * - 텍스트가 공백이면 검증 예외가 발생하고 인스턴스는 만들어지지 않는다.
 *
 * ## Input length
 * The factory rejects inputs longer than [MAX_TOKENIZE_TEXT_LENGTH] characters.
 * Callers that need a higher limit must construct [TokenizeRequest] directly and
 * perform their own length validation before calling the tokenize processors.
 *
 * ```kotlin
 * val request = tokenizeRequestOf("비동기 처리", TokenizeOptions())
 * // request.text == "비동기 처리"
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
