package io.bluetape4k.tokenizer.utils

import io.bluetape4k.logging.KLogging
import java.io.Serializable

/**
 * Mutable set that stores char-array-based keys without duplicates.
 *
 * ## Behavior / Contract
 * - Backed by a [CharArrayMap]; only a placeholder value is stored per entry.
 * - Accepts `Any`, `String`, `CharSequence`, and `CharArray` inputs.
 * - Optimized for high-frequency membership lookups such as word dictionaries and block-word lists.
 *
 * ```kotlin
 * val set = CharArraySet(4)
 * set.add("token")
 * // set.contains("token") == true
 * // set.size == 1
 * ```
 */
open class CharArraySet(val map: CharArrayMap<Any>): AbstractMutableSet<Any>(), Serializable {

    companion object: KLogging() {
        private val EMPTY_SET = CharArraySet(CharArrayMap.emptyMap())
        private val PLACEHOLDER = Any()

        @Suppress("USELESS_IS_CHECK")
        @JvmStatic
        /**
         * Returns a read-only view of [set].
         *
         * ## Behavior / Contract
         * - An empty set returns the shared `EMPTY_SET` singleton.
         * - All other sets are wrapped in an [CharArrayMap.UnmodifiableCharArrayMap]-backed instance.
         *
         * ```kotlin
         * val source = CharArraySet(2).apply { add("a") }
         * val readonly = CharArraySet.unmodifiableSet(source)
         * // readonly.contains("a") == true
         * // readonly.add("b") throws UnsupportedOperationException
         * ```
         */
        fun unmodifiableSet(set: CharArraySet): CharArraySet {
            return when (set) {
                EMPTY_SET -> EMPTY_SET
                // is CharArrayMap.UnmodifiableCharArrayMap<*> -> set
                else -> CharArraySet(CharArrayMap.unmodifiableMap(set.map))
            }
        }

        @JvmStatic
        /**
         * Creates a new [CharArraySet] by copying the contents of [set].
         *
         * ## Behavior / Contract
         * - When [set] is a [CharArraySet], uses the internal map copy constructor.
         * - When [set] is the shared empty singleton, returns the same singleton.
         *
         * ```kotlin
         * val copied = CharArraySet.copy(setOf("a", "b"))
         * // copied.contains("a") == true
         * // copied.size == 2
         * ```
         */
        fun copy(set: Set<Any>): CharArraySet = when (set) {
            EMPTY_SET -> EMPTY_SET
            is CharArraySet -> CharArraySet(CharArrayMap.copy(set.map))
            else      -> CharArraySet(set)
        }
    }

    /**
     * Creates a set with an initial capacity hint based on the expected number of elements.
     *
     * ## Behavior / Contract
     * - Internally creates a `CharArrayMap(startSize)`.
     * - The capacity is a hint to delay rehashing.
     *
     * ```kotlin
     * val set = CharArraySet(128)
     * // set.isEmpty() == true
     * ```
     */
    constructor(startSize: Int): this(CharArrayMap<Any>(startSize))

    /**
     * Creates a set by copying the contents of [c].
     *
     * ## Behavior / Contract
     * - Uses `c.size` for the initial capacity, then populates via `addAll`.
     * - Duplicate elements are deduplicated by set semantics.
     *
     * ```kotlin
     * val set = CharArraySet(listOf("a", "a", "b"))
     * // set.size == 2
     * ```
     */
    constructor(c: Collection<Any>): this(c.size) {
        @Suppress("LeakingThis")
        addAll(c)
    }

    /**
     * Removes all elements from the set.
     *
     * ## Behavior / Contract
     * - Delegates to [CharArrayMap.clear], resetting all key/value slots.
     * - After the call, [size] is 0.
     *
     * ```kotlin
     * val set = CharArraySet(2).apply { add("a") }
     * set.clear()
     * // set.isEmpty() == true
     * ```
     */
    override fun clear() {
        map.clear()
    }

    /**
     * Returns `true` if [element] exists in the set.
     *
     * ## Behavior / Contract
     * - Delegates to the internal map's key-comparison rules for any `Any` input.
     *
     * ```kotlin
     * val set = CharArraySet(2).apply { add("hello") }
     * // set.contains("hello") == true
     * ```
     */
    override fun contains(element: Any): Boolean = map.containsKey(element)

    /**
     * Returns `true` if the char-array slice `text[off, off+len)` exists in the set.
     *
     * ## Behavior / Contract
     * - Delegates to the internal map's slot search for the given range.
     *
     * ```kotlin
     * val set = CharArraySet(4).apply { add("token") }
     * val chars = "token".toCharArray()
     * // set.contains(chars, 0, chars.size) == true
     * ```
     */
    fun contains(text: CharArray, off: Int, len: Int = text.size) = map.containsKey(text, off, len)

    /**
     * Returns `true` if [cs] exists in the set.
     *
     * ## Behavior / Contract
     * - Comparison is performed on raw character content without case normalization.
     *
     * ```kotlin
     * val set = CharArraySet(2).apply { add("hello") }
     * // set.contains("hello") == true
     * ```
     */
    fun contains(cs: CharSequence) = map.containsKey(cs)

    /**
     * Adds [element] to the set.
     *
     * ## Behavior / Contract
     * - Stores a placeholder value in the internal map to register the element.
     * - Returns `true` only when the key was not already present.
     *
     * ```kotlin
     * val set = CharArraySet(2)
     * // set.add("x") == true
     * ```
     */
    override fun add(element: Any): Boolean = map.put(element, PLACEHOLDER) == null

    /**
     * Adds a [CharSequence] key to the set.
     *
     * ## Behavior / Contract
     * - Deduplication is based on character content.
     * - Returns `false` if the key already exists.
     *
     * ```kotlin
     * val set = CharArraySet(2)
     * // set.add("abc" as CharSequence) == true
     * ```
     */
    open fun add(text: CharSequence) = map.put(text, PLACEHOLDER) == null

    /**
     * Adds a [String] key to the set.
     *
     * ## Behavior / Contract
     * - Uses the internal map's string insert path.
     * - Duplicate keys are not inserted.
     *
     * ```kotlin
     * val set = CharArraySet(2)
     * // set.add("abc") == true
     * ```
     */
    open fun add(text: String) = map.put(text, PLACEHOLDER) == null

    /**
     * Adds a char-array key to the set.
     *
     * ## Behavior / Contract
     * - Stores the array reference directly as an internal key.
     * - Returns `false` if an equal char sequence is already present.
     *
     * ```kotlin
     * val set = CharArraySet(2)
     * // set.add("abc".toCharArray()) == true
     * ```
     */
    open fun add(text: CharArray) = map.put(text, PLACEHOLDER) == null

    /**
     * Adds all elements from [elements] to the set.
     *
     * ## Behavior / Contract
     * - Iterates over each element and calls [add].
     * - Returns `true` if at least one new element was added.
     *
     * ```kotlin
     * val set = CharArraySet(2)
     * // set.addAll(listOf("a", "b")) == true
     * ```
     */
    override fun addAll(elements: Collection<Any>): Boolean {
        var modified = false
        elements.forEach {
            if (add(it)) modified = true
        }
        return modified
    }

    /**
     * Removes [element] from the set.
     *
     * ## Behavior / Contract
     * - Returns `true` only when the element was actually present and removed.
     * - Returns `false` without throwing when the element is absent.
     *
     * ```kotlin
     * val set = CharArraySet(2).apply { add("a") }
     * // set.remove("a") == true
     * ```
     */
    override fun remove(element: Any): Boolean = map.remove(element) != null

    /**
     * Removes a [String] key from the set.
     *
     * ## Behavior / Contract
     * - Returns `true` only when the key was actually present and removed.
     * - Returns `false` when the key was absent.
     *
     * ```kotlin
     * val set = CharArraySet(2).apply { add("x") }
     * set.remove("x")
     * // set.contains("x") == false
     * ```
     */
    fun remove(text: String): Boolean = map.remove(text) != null

    /**
     * Removes all elements in [elements] from the set.
     *
     * ## Behavior / Contract
     * - Calls [remove] for each element.
     * - Returns `true` if at least one element was actually removed.
     *
     * ```kotlin
     * val set = CharArraySet(2).apply { addAll(listOf("a", "b")) }
     * // set.removeAll(listOf("a", "b")) == true
     * ```
     */
    override fun removeAll(elements: Collection<Any>): Boolean {
        var modified = false
        elements.forEach {
            if (remove(it)) modified = true
        }
        return modified
    }

    /**
     * Removes all strings in [words] from the set.
     *
     * ## Behavior / Contract
     * - Calls [remove] for each string in the list.
     * - Returns `true` if at least one element was actually removed.
     *
     * ```kotlin
     * val set = CharArraySet(2).apply { addAll(listOf("x", "y")) }
     * // set.removeAll(listOf("x", "y")) == true
     * ```
     */
    fun removeAll(words: List<String>): Boolean {
        var modified = false
        words.forEach {
            if (remove(it)) modified = true
        }
        return modified
    }

    /**
     * Returns the number of elements in the set.
     *
     * ## Behavior / Contract
     * - Delegates directly to the internal map's [size].
     * - Read-only; does not mutate state.
     *
     * ```kotlin
     * val set = CharArraySet(2).apply { add("one") }
     * // set.size == 1
     * ```
     */
    override val size: Int
        get() = map.size

    /**
     * Returns an iterator over the elements in the set.
     *
     * ## Behavior / Contract
     * - Reuses the internal map's original key set iterator.
     * - Returned elements are the raw `CharArray` key references.
     *
     * ```kotlin
     * val set = CharArraySet(2).apply { add("it") }
     * // set.iterator().hasNext() == true
     * ```
     */
    override fun iterator(): MutableIterator<Any> {
        return map.originalKeySet.iterator()
    }

    override fun toString(): String = buildString {
        append("[")
        this@CharArraySet.forEach { item ->
            if (this.length > 1) append(", ")
            when (item) {
                is CharArray -> append(item)
                else -> append(item.toString())
            }
        }
        append("]")
    }
}
