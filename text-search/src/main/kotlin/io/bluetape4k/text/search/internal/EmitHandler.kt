package io.bluetape4k.text.search.internal

/**
 * Callback invoked each time a keyword match is found during Aho-Corasick text parsing.
 *
 * Returning `false` signals the caller to stop processing (used with `stopOnHit`).
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
     * Called when a keyword match is found.
     *
     * @param emit matched keyword information (start, end, keyword)
     * @return `true` to continue processing; `false` to stop (when `stopOnHit` is active)
     */
    fun emit(emit: Emit): Boolean
}

/**
 * An [EmitHandler] that accumulates all matched emits into [emits].
 *
 * @see AbstractStatefulEmitHandler
 * @see DefaultEmitHandler
 */
internal interface StatefulEmitHandler: EmitHandler {
    /** Accumulated list of all matched emits. */
    val emits: MutableList<Emit>
}

/**
 * Abstract base implementation of [StatefulEmitHandler].
 *
 * Stores all emits in an internal list. Override [emit] to apply custom filtering logic.
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
    /** Accumulated emit list. */
    override val emits: MutableList<Emit> = mutableListOf()

    /**
     * Appends [emit] to the list and returns `true`.
     *
     * @param emit the emit to add
     */
    fun addEmit(emit: Emit): Boolean = emits.add(emit)
}

/**
 * Default [StatefulEmitHandler] that stores every emit without filtering.
 */
internal class DefaultEmitHandler: AbstractStatefulEmitHandler() {
    /** Appends [emit] to the list and returns `true`. */
    override fun emit(emit: Emit): Boolean = addEmit(emit)
}
