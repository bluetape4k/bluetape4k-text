package io.bluetape4k.tokenizer.model

/**
 * Block-word 처리 결과와 감지한 단어 목록을 담는 response model입니다.
 *
 * ## 동작 계약
 * - [maskedText]는 caller가 전달한 최종 masked string을 저장합니다.
 * - [blockWords]의 기본값은 empty list이며 감지한 단어가 없음을 뜻합니다.
 * - [blockwordExists]는 `blockWords.isNotEmpty()`에서 계산하는 property입니다.
 *
 * @property request 이 response를 만든 원본 [BlockwordRequest]입니다.
 * @property maskedText 금칙어가 masking된 최종 문자열입니다.
 * @property blockWords 감지한 금칙어 목록입니다. 감지 결과가 없으면 empty list입니다.
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
     * 감지한 block word가 하나 이상이면 `true`를 반환합니다.
     *
     * ## 동작 계약
     * - [blockWords]가 비어 있지 않으면 `true`를 반환합니다.
     * - Computed property이며 별도 state를 저장하지 않습니다.
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
 * 전달한 [request], [maskedText], [blockWords]로 [BlockwordResponse]를 만듭니다.
 *
 * ## 동작 계약
 * - 모든 parameter를 새 [BlockwordResponse]에 그대로 매핑합니다.
 * - [blockWords]를 생략하면 empty list를 사용합니다.
 *
 * @param request 원본 blockword request입니다.
 * @param maskedText 금칙어가 masking된 최종 문자열입니다.
 * @param blockWords 감지한 금칙어 목록입니다. 생략하면 empty list입니다.
 * @return 전달한 결과 값을 담은 [BlockwordResponse]입니다.
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
