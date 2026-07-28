package io.bluetape4k.text.search.internal

/**
 * Aho-Corasick text parsing 중 키워드 match를 찾을 때마다 호출되는 콜백입니다.
 *
 * `false`를 반환하면 호출자에게 처리를 중단하라고 알립니다. 이 동작은 `stopOnHit`과 함께 사용합니다.
 *
 * ```
 * val handler = EmitHandler { emit ->
 *     println("Found: ${emit.keyword} at position ${emit.start}-${emit.end}")
 *     true
 * }
 * trieCore.runParseText(text, handler)
 * ```
 *
 * @see StatefulEmitHandler
 * @see DefaultEmitHandler
 */
internal fun interface EmitHandler {
    /**
     * 키워드 match를 찾았을 때 호출됩니다.
     *
     * @param emit match된 키워드 정보입니다. Start, end, keyword를 포함합니다.
     * @return 계속 처리하려면 `true`, 중단하려면 `false`를 반환합니다. 중단은 `stopOnHit`이 켜져 있을 때 사용합니다.
     */
    fun emit(emit: Emit): Boolean
}

/**
 * Match된 emit을 모두 [emits]에 누적하는 [EmitHandler]입니다.
 *
 * @see AbstractStatefulEmitHandler
 * @see DefaultEmitHandler
 */
internal interface StatefulEmitHandler: EmitHandler {
    /** Match된 emit을 모두 누적한 list입니다. */
    val emits: MutableList<Emit>
}

/**
 * [StatefulEmitHandler]의 추상 기본 구현입니다.
 *
 * 모든 emit을 내부 list에 저장합니다. 맞춤 필터링 로직을 적용하려면 [emit]을 override합니다.
 *
 * ```
 * val handler = object : AbstractStatefulEmitHandler() {
 *     override fun emit(emit: Emit): Boolean {
 *         if ((emit.keyword?.length ?: 0) >= 3) {
 *             return addEmit(emit)
 *         }
 *         return false
 *     }
 * }
 * ```
 */
internal abstract class AbstractStatefulEmitHandler: StatefulEmitHandler {
    /** 누적된 emit list입니다. */
    override val emits: MutableList<Emit> = mutableListOf()

    /**
     * [emit]을 list에 추가하고 `true`를 반환합니다.
     *
     * @param emit 추가할 emit입니다.
     * @return list에 추가되면 `true`입니다.
     */
    fun addEmit(emit: Emit): Boolean = emits.add(emit)
}

/**
 * 필터링 없이 모든 emit을 저장하는 기본 [StatefulEmitHandler]입니다.
 */
internal class DefaultEmitHandler: AbstractStatefulEmitHandler() {
    /** [emit]을 list에 추가하고 `true`를 반환합니다. */
    override fun emit(emit: Emit): Boolean = addEmit(emit)
}
