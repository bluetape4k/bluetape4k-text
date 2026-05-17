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
 * 금칙어 탐지/마스킹 처리를 요청하는 입력 모델이다.
 *
 * ## 동작/계약
 * - 생성 시 `text.requireNotBlank("text")`를 검사해 공백 문자열 요청을 거부한다.
 * - `options`를 지정하지 않으면 `BlockwordOptions.DEFAULT`를 사용한다.
 * - `AbstractMessage`를 상속하므로 생성 타임스탬프가 함께 기록된다.
 *
 * ```kotlin
 * val request = blockwordRequestOf("나쁜 단어", blockwordOptionsOf(mask = "*"))
 * // request.text == "나쁜 단어"
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
 * 금칙어 처리 요청 인스턴스를 생성한다.
 *
 * ## 동작/계약
 * - 생성 전에 `text.requireNotBlank("text")`를 호출해 비어 있거나 공백인 입력을 차단한다.
 * - `text.length > MAX_BLOCKWORD_TEXT_LENGTH`이면 `IllegalArgumentException`을 던진다.
 * - 검증을 통과하면 `BlockwordRequest(text, options)`를 반환한다.
 *
 * ## Input length
 * The factory rejects inputs longer than [MAX_BLOCKWORD_TEXT_LENGTH] characters.
 * Callers that need a higher limit must construct [BlockwordRequest] directly and
 * perform their own length validation before calling the blockword processors.
 *
 * ```kotlin
 * val request = blockwordRequestOf("테스트 문장")
 * // request.text == "테스트 문장"
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
