package io.bluetape4k.text.search.internal

import io.bluetape4k.ValueObject
import io.bluetape4k.logging.KLogging
import java.util.*

/**
 * A single state in the Aho-Corasick automaton trie.
 *
 * Maintains success transitions, a failure link, and the set of emitted keywords.
 * State transitions are performed via [nextState] and [addState].
 *
 * ```kotlin
 * val root = State()
 * val next = root.addState('a')
 * // next.depth == 1
 * ```
 *
 * @property depth depth of this state from the root
 */
internal class State(val depth: Int = 0): ValueObject {

    companion object: KLogging()

    private val rootState: State? get() = if (depth == 0) this else null
    private val success = mutableMapOf<Char, State>()
    private val emits = TreeSet<String>()

    var failure: State? = null

    /** Looks up the transition for [ch]. On a miss at the root state, returns the root itself (unless [ignoreRootState]). */
    fun nextState(ch: Char, ignoreRootState: Boolean = false): State? {
        var nextState = this.success[ch]

        val canUseRootState = !ignoreRootState && nextState == null && rootState != null
        if (canUseRootState) {
            nextState = rootState
        }
        return nextState
    }

    /** Looks up the transition for [ch] without falling back to the root state. */
    fun nextStateIgnoreRootState(ch: Char): State? = nextState(ch, true)

    /** Traverses or creates states for each character in [keyword] and returns the final state. */
    fun addState(keyword: String): State {
        var state = this
        keyword.forEach { state = state.addState(it) }
        return state
    }

    /** Adds a single-character transition for [ch], or returns the existing target state. */
    fun addState(ch: Char): State {
        var nextState = nextStateIgnoreRootState(ch)
        if (nextState == null) {
            nextState = State(this.depth + 1)
            success[ch] = nextState
        }
        return nextState
    }

    /** Adds transitions for each character in [chars] in order and returns the final state. */
    fun addStates(vararg chars: Char): State {
        var state = this
        chars.forEach {
            state = state.addState(it)
        }
        return state
    }

    /** Adds a keyword emit to this state. */
    fun addEmit(keyword: String) {
        this.emits.add(keyword)
    }

    /** Adds all emits from [emits] collection to this state. */
    fun addEmits(emits: Collection<String>) {
        this.emits.addAll(emits)
    }

    /** Adds all emits from vararg [emits] to this state. */
    fun addEmits(vararg emits: String) {
        this.emits.addAll(emits)
    }

    /** Returns the set of keyword emits for this state. */
    fun emit(): Collection<String> = this.emits

    /** Returns the collection of child states. */
    fun getStates(): Collection<State> = this.success.values

    /** Returns the collection of characters with outgoing transitions. */
    fun getTransitions(): Collection<Char> = this.success.keys

    override fun toString(): String = "State(emits=$emits, failure=$failure)"
}
