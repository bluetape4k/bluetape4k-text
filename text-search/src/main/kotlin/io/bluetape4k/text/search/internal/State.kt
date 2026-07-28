package io.bluetape4k.text.search.internal

import io.bluetape4k.ValueObject
import io.bluetape4k.logging.KLogging
import java.util.*

/**
 * Aho-Corasick automaton trie의 단일 상태입니다.
 *
 * 성공 전이, failure link, 이 상태에서 emit되는 keyword 집합을 보관합니다. 상태 전이는 [nextState]와
 * [addState]로 수행합니다.
 *
 * ```kotlin
 * val root = State()
 * val next = root.addState('a')
 * // next.depth == 1
 * ```
 *
 * @property depth root에서 이 상태까지의 깊이입니다.
 */
internal class State(val depth: Int = 0): ValueObject {

    companion object: KLogging()

    private val rootState: State? get() = if (depth == 0) this else null
    private val success = mutableMapOf<Char, State>()
    private val emits = TreeSet<String>()

    var failure: State? = null

    /**
     * [ch] 전이를 조회합니다.
     *
     * @param ch 조회할 전이 문자입니다.
     * @param ignoreRootState `true`이면 root 상태에서 miss가 나도 root 자신을 반환하지 않습니다.
     * @return 전이 대상 상태입니다. Root에서 자기 자신으로 되돌아가는 경로도 없으면 `null`입니다.
     */
    fun nextState(ch: Char, ignoreRootState: Boolean = false): State? {
        var nextState = this.success[ch]

        val canUseRootState = !ignoreRootState && nextState == null && rootState != null
        if (canUseRootState) {
            nextState = rootState
        }
        return nextState
    }

    /** Root에서 자기 자신으로 되돌아가지 않고 [ch] 전이를 조회합니다. */
    fun nextStateIgnoreRootState(ch: Char): State? = nextState(ch, true)

    /**
     * [keyword]의 각 문자를 따라 상태를 순회하거나 생성하고 마지막 상태를 반환합니다.
     *
     * @param keyword trie에 추가할 keyword입니다.
     * @return [keyword]의 마지막 문자를 나타내는 상태입니다.
     */
    fun addState(keyword: String): State {
        var state = this
        keyword.forEach { state = state.addState(it) }
        return state
    }

    /**
     * [ch]에 대한 단일 문자 전이를 추가하거나 기존 대상 상태를 반환합니다.
     *
     * @param ch 추가할 전이 문자입니다.
     * @return [ch] 전이가 가리키는 상태입니다.
     */
    fun addState(ch: Char): State {
        var nextState = nextStateIgnoreRootState(ch)
        if (nextState == null) {
            nextState = State(this.depth + 1)
            success[ch] = nextState
        }
        return nextState
    }

    /**
     * [chars]의 각 문자를 순서대로 전이로 추가하고 마지막 상태를 반환합니다.
     *
     * @param chars 추가할 전이 문자들입니다.
     * @return 마지막 전이 문자가 가리키는 상태입니다.
     */
    fun addStates(vararg chars: Char): State {
        var state = this
        chars.forEach {
            state = state.addState(it)
        }
        return state
    }

    /** 이 상태에 keyword emit을 추가합니다. */
    fun addEmit(keyword: String) {
        this.emits.add(keyword)
    }

    /** [emits] collection의 모든 keyword emit을 이 상태에 추가합니다. */
    fun addEmits(emits: Collection<String>) {
        this.emits.addAll(emits)
    }

    /** Vararg [emits]의 모든 keyword emit을 이 상태에 추가합니다. */
    fun addEmits(vararg emits: String) {
        this.emits.addAll(emits)
    }

    /** 이 상태에서 emit되는 keyword 집합을 반환합니다. */
    fun emit(): Collection<String> = this.emits

    /** 이 상태에서 성공 전이로 도달할 수 있는 자식 상태 collection을 반환합니다. */
    fun getStates(): Collection<State> = this.success.values

    /** 이 상태에서 나가는 전이 문자 collection을 반환합니다. */
    fun getTransitions(): Collection<Char> = this.success.keys

    override fun toString(): String = "State(emits=$emits, failure=$failure)"
}
