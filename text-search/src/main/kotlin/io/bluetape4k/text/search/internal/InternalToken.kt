package io.bluetape4k.text.search.internal

import io.bluetape4k.logging.KLogging
import java.io.Serializable

/**
 * Common contract for an internal tokenization fragment.
 *
 * [fragment] holds a substring of the original text. A non-null [emit] indicates a keyword match.
 *
 * ```kotlin
 * val token: InternalToken = MatchToken("PM", Emit(0, 1, "PM"))
 * // token.isMatch() == true
 * ```
 *
 * @property fragment substring of the original text
 * @property emit emit information, or `null` for non-matching fragments
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
 * An [InternalToken] that carries a keyword [Emit]. [isMatch] always returns `true`.
 *
 * @property emit the associated emit
 */
internal class MatchToken(fragment: String, override val emit: Emit): AbstractInternalToken(fragment) {
    companion object : KLogging() {
        private const val serialVersionUID = 1L
    }

    override fun isMatch(): Boolean = true
}

/**
 * An [InternalToken] for a non-matching fragment. [isMatch] always returns `false`; [emit] is always `null`.
 */
internal class FragmentToken(fragment: String): AbstractInternalToken(fragment) {
    companion object : KLogging() {
        private const val serialVersionUID = 1L
    }

    override fun isMatch(): Boolean = false
    override val emit: Emit? = null
}
