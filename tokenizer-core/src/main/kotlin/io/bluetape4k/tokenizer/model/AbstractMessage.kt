package io.bluetape4k.tokenizer.model

import io.bluetape4k.logging.KLogging
import java.io.Serializable

/**
 * Tokenizer request와 response message에 creation timestamp를 제공하는 abstract base입니다.
 *
 * ## 동작 계약
 * - [timestamp]는 construction time에 `System.currentTimeMillis()`로 설정됩니다.
 * - [timestamp]는 immutable(`val`)이며 생성 후 바뀌지 않습니다.
 * - Serializable message hierarchy를 지원하기 위해 [Serializable]을 구현합니다.
 *
 * ```kotlin
 * val request = tokenizeRequestOf("hello")
 * val response = tokenizeResponseOf(request.text, listOf("hello"))
 * // request.timestamp > 0L
 * // response.timestamp >= request.timestamp
 * ```
 */
abstract class AbstractMessage: Serializable {

    companion object : KLogging() {
        private const val serialVersionUID = 1L
    }


    /**
     * 이 message instance가 생성된 epoch millisecond timestamp입니다.
     *
     * ## 동작 계약
     * - Construction time에 정확히 한 번 초기화됩니다.
     * - 같은 instance에서 반복해서 읽으면 항상 같은 값을 반환합니다.
     *
     * ```kotlin
     * val message = tokenizeRequestOf("hello")
     * val first = message.timestamp
     * val second = message.timestamp
     * // first == second
     * ```
     */
    val timestamp: Long = System.currentTimeMillis()

}
