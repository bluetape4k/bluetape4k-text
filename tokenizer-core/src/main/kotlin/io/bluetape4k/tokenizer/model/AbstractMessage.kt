package io.bluetape4k.tokenizer.model

import io.bluetape4k.logging.KLogging
import java.io.Serializable

/**
 * Abstract base that provides a creation timestamp for tokenizer request and response messages.
 *
 * ## Behavior / Contract
 * - [timestamp] is set to `System.currentTimeMillis()` at construction time.
 * - [timestamp] is immutable (`val`) and cannot change after creation.
 * - Implements [Serializable] to support the serializable message hierarchy.
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
     * The epoch-millisecond timestamp at which this message instance was created.
     *
     * ## Behavior / Contract
     * - Initialized exactly once at construction time.
     * - Repeated reads on the same instance always return the same value.
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
