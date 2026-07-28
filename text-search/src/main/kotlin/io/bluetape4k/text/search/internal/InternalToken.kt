package io.bluetape4k.text.search.internal

import io.bluetape4k.logging.KLogging
import java.io.Serializable

/**
 * 내부 tokenization fragment의 공통 계약입니다.
 *
 * [fragment]는 원본 문자열의 substring을 보관합니다. [emit]이 non-null이면 키워드 match를 뜻합니다.
 *
 * ```kotlin
 * val token: InternalToken = MatchToken("PM", Emit(0, 1, "PM"))
 * // token.isMatch() == true
 * ```
 *
 * @property fragment 원본 문자열의 substring입니다.
 * @property emit emit 정보입니다. 비매치 fragment이면 `null`입니다.
 */
internal sealed interface InternalToken: Serializable {
    val fragment: String
    val emit: Emit?

    fun isMatch(): Boolean
}

internal abstract class AbstractInternalToken(override val fragment: String): InternalToken {
    companion object : KLogging() {
        private const val serialVersionUID = 1L
    }

    override fun toString(): String = "InternalToken(fragment=$fragment, emit=$emit)"
}

/**
 * 키워드 [Emit]을 담는 [InternalToken]입니다. [isMatch]는 항상 `true`를 반환합니다.
 *
 * @property emit 연결된 emit입니다.
 */
internal class MatchToken(fragment: String, override val emit: Emit): AbstractInternalToken(fragment) {
    companion object : KLogging() {
        private const val serialVersionUID = 1L
    }

    override fun isMatch(): Boolean = true
}

/**
 * 비매치 fragment용 [InternalToken]입니다. [isMatch]는 항상 `false`를 반환하고 [emit]은 항상 `null`입니다.
 */
internal class FragmentToken(fragment: String): AbstractInternalToken(fragment) {
    companion object : KLogging() {
        private const val serialVersionUID = 1L
    }

    override fun isMatch(): Boolean = false
    override val emit: Emit? = null
}
