package io.bluetape4k.tokenizer.utils

import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.requireNotNull
import java.io.Serializable

/**
 * Open-addressing hash map optimized for `CharArray` keys.
 *
 * ## Behavior / Contract
 * - Accepts `Any` as a key but normalizes it internally to a `CharArray` or string representation.
 * - Collision resolution uses an increment-based (`inc`) probe strategy rather than linear probing.
 * - On removal, remaining entries are reinserted to preserve the probe chain.
 *
 * ```kotlin
 * val map = CharArrayMap<Int>(4)
 * map.put("token", 1)
 * // map["token"] == 1
 * // map.containsKey("token") == true
 * ```
 */
@Suppress("UNCHECKED_CAST")
open class CharArrayMap<V>(startSize: Int): AbstractMutableMap<Any, V>(), Serializable {

    companion object: KLogging() {
        private const val INIT_SIZE = 8
        private val EMPTY_MAP: CharArrayMap<Any> = EmptyCharArrayMap()

        @JvmStatic
        /**
         * Returns a read-only view of the given map that blocks all mutation operations.
         *
         * ## Behavior / Contract
         * - Empty maps return the shared [emptyMap] singleton.
         * - A map that is already an [UnmodifiableCharArrayMap] is returned as-is.
         * - Otherwise, wraps the map in [UnmodifiableCharArrayMap].
         *
         * ## Aliasing Warning
         * Like [java.util.Collections.unmodifiableMap], the returned view shares the backing
         * arrays of the original map. If the caller retains a reference to the original mutable
         * map and continues to write to it, those changes will be visible through the
         * unmodifiable view. To prevent this, discard the original reference after calling
         * `unmodifiableMap`:
         *
         * ```kotlin
         * var source: CharArrayMap<Int> = CharArrayMap<Int>(2).apply { put("a", 1) }
         * val readonly = CharArrayMap.unmodifiableMap(source)
         * source = CharArrayMap(0)  // discard original — readonly is now stable
         * // readonly["a"] == 1
         * // readonly.put("b", 2) throws UnsupportedOperationException
         * ```
         */
        fun <V> unmodifiableMap(map: CharArrayMap<V>): CharArrayMap<V> {
            return when {
                map.isEmpty() -> emptyMap()
                else          -> when (map) {
                    is UnmodifiableCharArrayMap -> map
                    else                        -> UnmodifiableCharArrayMap(map)
                }
            }
        }

        @JvmStatic
        /**
         * Copies a standard [Map] into a new [CharArrayMap].
         *
         * ## Behavior / Contract
         * - All entries from the source map are loaded via `putAll`.
         * - The source map and the result are independent instances.
         *
         * ```kotlin
         * val copied = CharArrayMap.copy(mapOf<Any, Int>("x" to 10))
         * // copied["x"] == 10
         * ```
         */
        fun <V> copy(map: Map<Any, V>): CharArrayMap<V> = CharArrayMap(map)

        @JvmStatic
        /**
         * Returns the shared immutable empty-map singleton.
         *
         * ## Behavior / Contract
         * - Always returns the same internal `EMPTY_MAP` instance cast to the requested type.
         * - Insert and remove operations are not supported.
         *
         * ```kotlin
         * val empty = CharArrayMap.emptyMap<Int>()
         * // empty.isEmpty() == true
         * ```
         */
        fun <V> emptyMap(): CharArrayMap<V> = EMPTY_MAP as CharArrayMap<V>
    }

    @Suppress("LeakingThis")
    /**
     * Creates a map by copying all entries from [c].
     *
     * ## Behavior / Contract
     * - Initial capacity is derived from `c.size`.
     * - All entries are loaded via `putAll(c)` at construction time.
     *
     * ```kotlin
     * val map = CharArrayMap(mapOf<Any, Int>("a" to 1))
     * // map.size == 1
     * ```
     */
    constructor(c: Map<Any, V>): this(c.size) {
        putAll(c)
    }

    /**
     * Creates a shallow copy sharing the backing arrays of [src].
     *
     * ## Behavior / Contract
     * - The `_keys` and `_values` array references are shared directly.
     * - Mutations through either map will be visible in the other if both remain mutable.
     *
     * ```kotlin
     * val source = CharArrayMap<Int>(2).apply { put("a", 1) }
     * val copy = CharArrayMap(source)
     * // copy["a"] == 1
     * ```
     */
    constructor(src: CharArrayMap<V>): this(0) {
        this._keys = src._keys
        this._values = src._values
        this._count = src._count
        this.charUtils = src.charUtils
    }

    private var charUtils: CharacterUtils = CharacterUtils.getInstance()
    private var _count: Int = 0
    private var _keys: Array<CharArray?>
    private var _values: Array<V?>

    init {
        var size = INIT_SIZE
        while (startSize + (startSize shr 2) > size) {
            size = size shl 1
        }
        _keys = arrayOfNulls(size)
        _values = arrayOfNulls<Any>(size) as Array<V?>
    }

    /**
     * Removes all entries from the map.
     *
     * ## Behavior / Contract
     * - Sets all slots in the key and value arrays to `null`.
     * - Resets `_count` to 0.
     *
     * ```kotlin
     * val map = CharArrayMap<Int>(2).apply { put("a", 1) }
     * map.clear()
     * // map.isEmpty() == true
     * ```
     */
    override fun clear() {
        _count = 0
        _keys.fill(null)
        _values.fill(null)
    }

    /**
     * Returns whether the char-array slice `text[off, off+len)` exists as a key.
     *
     * ## Behavior / Contract
     * - Computes the hash slot for the given range and checks for a non-null entry.
     *
     * ```kotlin
     * val map = CharArrayMap<Int>(2).apply { put("ab", 1) }
     * val chars = "ab".toCharArray()
     * // map.containsKey(chars, 0, chars.size) == true
     * ```
     */
    open fun containsKey(text: CharArray, off: Int, len: Int): Boolean {
        return _keys[getSlot(text, off, len)] != null
    }

    /**
     * Returns whether [cs] exists as a key.
     *
     * ## Behavior / Contract
     * - Computes the slot based on the character content of [cs].
     *
     * ```kotlin
     * val map = CharArrayMap<Int>(2).apply { put("key", 1) }
     * // map.containsKey("key") == true
     * ```
     */
    open fun containsKey(cs: CharSequence): Boolean = _keys[getSlot(cs)] != null

    /**
     * Returns whether [key] exists in the map.
     *
     * ## Behavior / Contract
     * - `CharArray` keys use the array lookup path.
     * - All other types use `toString()` for the lookup.
     *
     * ```kotlin
     * val map = CharArrayMap<Int>(2).apply { put("a", 1) }
     * // map.containsKey("a") == true
     * ```
     */
    override fun containsKey(key: Any): Boolean = when (key) {
        is CharArray -> containsKey(key, 0, key.size)
        else         -> containsKey(key.toString())
    }

    /**
     * Returns the value associated with the char-array slice `text[off, off+len)`, or `null` if absent.
     *
     * ## Behavior / Contract
     * - Key comparison is performed character by character.
     *
     * ```kotlin
     * val map = CharArrayMap<Int>(2).apply { put("x", 3) }
     * // map.get("x".toCharArray(), 0, 1) == 3
     * ```
     */
    open fun get(text: CharArray, off: Int, len: Int): V? {
        return _values[getSlot(text, off, len)]
    }

    /**
     * Returns the value associated with [cs], or `null` if absent.
     *
     * ## Behavior / Contract
     * - Slot lookup is performed using the string content of [cs].
     *
     * ```kotlin
     * val map = CharArrayMap<Int>(2).apply { put("y", 7) }
     * // map.get("y") == 7
     * ```
     */
    open fun get(cs: CharSequence): V? = _values[getSlot(cs)]

    /**
     * Returns the value associated with [key], or `null` if absent.
     *
     * ## Behavior / Contract
     * - `CharArray` keys use the array path; all other types use `toString()`.
     *
     * ```kotlin
     * val map = CharArrayMap<Int>(2).apply { put("b", 2) }
     * // map["b"] == 2
     * ```
     */
    override fun get(key: Any): V? = when (key) {
        is CharArray -> get(key, 0, key.size)
        else         -> get(key.toString())
    }

    private fun getSlot(text: CharArray, off: Int, len: Int = text.size): Int {
        var code = getHashCode(text, off, len)
        var pos = code and (_keys.size - 1)
        var text2 = _keys[pos]

        fun isTextDifferent(text2: CharArray?): Boolean = text2?.let { !equals(text, off, len, text2) } ?: false

        val inc = ((code shr 8) + code) or 1
        while (isTextDifferent(text2)) {
            code += inc
            pos = code and (_keys.size - 1)
            text2 = _keys[pos]
        }

        return pos
    }

    private fun getSlot(text: CharSequence): Int {
        return getSlot(text.toString().toCharArray(), 0, text.length)
    }

    /**
     * Stores a [CharSequence] key and its associated value.
     *
     * ## Behavior / Contract
     * - Converts the key to a String and delegates to the String put path.
     * - Returns the previous value if the key already existed.
     *
     * ```kotlin
     * val map = CharArrayMap<Int>(2)
     * map.put("z", 1)
     * // map.put("z", 2) == 1
     * ```
     */
    open fun put(text: CharSequence, value: V): V? = put(text.toString(), value)

    /**
     * Stores an arbitrary key and its associated value.
     *
     * ## Behavior / Contract
     * - `CharArray` keys use the array insert path; all other types use `toString()`.
     *
     * ```kotlin
     * val map = CharArrayMap<Int>(2)
     * map.put("id", 1)
     * // map["id"] == 1
     * ```
     */
    override fun put(key: Any, value: V): V? = when (key) {
        is CharArray -> put(key, value)
        else         -> put(key.toString(), value)
    }

    /**
     * Stores a String key and its associated value.
     *
     * ## Behavior / Contract
     * - Converts the key to a `CharArray` and delegates to the array put path.
     * - Returns the previous value if the key already existed.
     *
     * ```kotlin
     * val map = CharArrayMap<Int>(2)
     * // map.put("k", 9) == null
     * ```
     */
    open fun put(text: String, value: V): V? = put(text.toCharArray(), value)

    /**
     * Stores a char-array key and its associated value.
     *
     * ## Behavior / Contract
     * - Replaces the existing value and returns the old one if the key is already present.
     * - For a new key, increments `_count` and triggers `rehash` when the load factor (~0.8) is exceeded.
     * - The key array is stored by reference; external mutations after insertion will affect map behaviour.
     *
     * ```kotlin
     * val map = CharArrayMap<Int>(2)
     * val key = "id".toCharArray()
     * map.put(key, 10)
     * // map["id"] == 10
     * ```
     */
    open fun put(text: CharArray, value: V): V? {
        val slot = getSlot(text, 0, text.size)

        _keys[slot]?.let {
            val oldValue = _values[slot]
            _values[slot] = value
            return oldValue
        }

        _keys[slot] = text
        _values[slot] = value
        _count++

        if (_count + (_count shr 2) > _keys.size) {
            rehash()
        }
        return null
    }

    private fun rehash() {
        require(_keys.size == _values.size) {
            "keys size [${_keys.size}] must equals to _values size[${_values.size}"
        }

        val newSize = 2 * _keys.size
        val oldKeys = _keys
        val oldValues = _values

        _keys = arrayOfNulls(newSize)
        _values = arrayOfNulls<Any>(newSize) as Array<V?>

        oldKeys.forEachIndexed { i, text ->
            text?.let {
                // Rehash: keys are unique, but getSlot still probes for an empty slot via char comparison.
                // A collision-free probe (compare only nulls) would be faster here; left as a future optimization.
                val slot = getSlot(text, 0, text.size)
                _keys[slot] = text
                _values[slot] = oldValues[i]
            }
        }
        //        oldKeys.fill(null)
        //        oldValues.fill(null)
    }

    private fun equals(text1: CharArray, off: Int, len: Int, text2: CharArray): Boolean {
        if (len != text2.size)
            return false

        repeat(len) {
            if (text1[off + it] != text2[it])
                return false
        }
        return true
    }

    private fun equals(text1: CharSequence, text2: CharArray): Boolean {
        val len = text1.length
        if (len != text2.size)
            return false

        repeat(len) {
            if (text1[it] != text2[it])
                return false
        }
        return true
    }

    private fun getHashCode(text: CharArray, offset: Int, len: Int): Int {
        var code = 0
        val stop = offset + len

        for (i in offset until stop) {
            code = code * 31 + text[i].code
        }
        return code
    }

    private fun getHashCode(text: CharSequence): Int {
        var code = 0
        val len = text.length

        for (i in 0 until len) {
            code = code * 31 + text[i].code
        }
        return code
    }

    /**
     * Removes [key] from the map and returns its previous value, or `null` if absent.
     *
     * ## Behavior / Contract
     * - When the key exists, all remaining entries are reinserted to rebuild the probe chain.
     * - Returns the removed value on success.
     *
     * ```kotlin
     * val map = CharArrayMap<Int>(2).apply { put("a", 1) }
     * val removed = map.remove("a")
     * // removed == 1
     * // map.containsKey("a") == false
     * ```
     */
    override fun remove(key: Any): V? {
        val keyChars = when (key) {
            is CharArray -> key
            else         -> key.toString().toCharArray()
        }
        val slot = getSlot(keyChars, 0, keyChars.size)
        if (_keys[slot] == null) return null  // 키가 존재하지 않음

        val oldValue = _values[slot]

        // 삭제 대상을 제외한 모든 엔트리를 수집
        @Suppress("UNCHECKED_CAST")
        val entries = mutableListOf<Pair<CharArray, V>>()
        for (i in _keys.indices) {
            if (i != slot && _keys[i] != null) {
                @Suppress("UNCHECKED_CAST")
                entries.add(_keys[i].requireNotNull("_keys[$i]") to (_values[i] as V))
            }
        }

        // Open addressing에서 삭제 시 probe chain이 끊어지므로 전체 재구성
        clear()
        for ((k, v) in entries) {
            put(k, v)
        }

        return oldValue
    }

    @Suppress("PROPERTY_HIDES_JAVA_FIELD")
    /**
     * The number of entries currently stored in the map.
     *
     * ## Behavior / Contract
     * - Incremented on insert; decremented or recomputed on `clear` / `remove`.
     * - Directly reflects the internal `_count` counter.
     *
     * ```kotlin
     * val map = CharArrayMap<Int>(2)
     * map.put("a", 1)
     * // map.size == 1
     * ```
     */
    override val size: Int get() = _count

    override fun toString(): String = buildString {
        append("{")
        this@CharArrayMap.entries.forEach { entry ->
            if (length > 1) append(", ")
            append(entry)
        }
        append("}")
    }

    private val _entrySet: EntrySet by lazy { createEntrySet() }

    /**
     * Extension point responsible for creating the entry set view.
     *
     * ## Behavior / Contract
     * - The default implementation returns a mutable `EntrySet(true)`.
     * - Read-only subclasses should override this to return an unmodifiable set.
     *
     * ```kotlin
     * val map = CharArrayMap<Int>(2)
     * // map.entries.isEmpty() == true
     * ```
     */
    protected open fun createEntrySet(): EntrySet = EntrySet(true)

    /**
     * Returns the entry set view of this map.
     *
     * ## Behavior / Contract
     * - Reuses the lazily initialized `_entrySet` instance.
     * - Mutability of the returned view depends on the [createEntrySet] implementation.
     *
     * ```kotlin
     * val map = CharArrayMap<Int>(2).apply { put("a", 1) }
     * // map.entries.first().value == 1
     * ```
     */
    override val entries: MutableSet<MutableMap.MutableEntry<Any, V>> get() = _entrySet

    private val _keySet: CharArraySet by lazy {
        object: CharArraySet(this@CharArrayMap as CharArrayMap<Any>) {
            override fun add(element: Any): Boolean = throw UnsupportedOperationException()
            override fun add(text: CharSequence): Boolean = throw UnsupportedOperationException()
            override fun add(text: String): Boolean = throw UnsupportedOperationException()
            override fun add(text: CharArray): Boolean = throw UnsupportedOperationException()
        }
    }

    /**
     * A read-only key set that exposes the raw `CharArray` key references without copying.
     *
     * ## Behavior / Contract
     * - Iteration returns direct references to the internal `_keys` array slots.
     * - Add and remove operations are not supported.
     *
     * ```kotlin
     * val map = CharArrayMap<Int>(2).apply { put("x", 1) }
     * val firstKey = map.originalKeySet.first() as CharArray
     * // String(firstKey) == "x"
     * ```
     */
    val originalKeySet: MutableSet<Any> by lazy {
        object: AbstractMutableSet<Any>() {
            override fun iterator(): MutableIterator<Any> = object: MutableIterator<Any> {

                private var pos = -1
                private var lastPos: Int = 0

                private fun goNext() {
                    lastPos = pos
                    pos++
                    while (pos < _keys.size && _keys[pos] == null) pos++
                }

                init {
                    goNext()
                }

                override fun hasNext(): Boolean = pos < _keys.size
                override fun next(): Any {
                    goNext()
                    return _keys[lastPos].requireNotNull("_keys[lastPos=$lastPos]")
                }

                override fun remove() = throw UnsupportedOperationException()
            }

            override fun add(element: Any): Boolean = throw UnsupportedOperationException()

            override val size: Int
                get() = this@CharArrayMap.size

            override fun contains(element: Any): Boolean = this@CharArrayMap.containsKey(element)
        }
    }

    /**
     * Returns the standard `Map.keys` view.
     *
     * ## Behavior / Contract
     * - Key additions are not permitted; use for lookups only.
     * - Backed internally by a `CharArraySet` wrapper.
     *
     * ```kotlin
     * val map = CharArrayMap<Int>(2).apply { put("k", 3) }
     * // map.keys.contains("k") == true
     * ```
     */
    override val keys: MutableSet<Any> get() = _keySet

    /**
     * Iterator that traverses internal slots and produces map entries.
     *
     * ## Behavior / Contract
     * - When `allowModify` is `false`, [setValue] throws [UnsupportedOperationException].
     * - [nextKey] returns the raw internal `CharArray` reference.
     * - [remove] is not supported.
     *
     * ```kotlin
     * val map = CharArrayMap<Int>(2).apply { put("a", 1) }
     * val it = map.EntryIterator(true)
     * // it.hasNext() == true
     * ```
     */
    inner class EntryIterator(private val allowModify: Boolean):
        MutableIterator<MutableMap.MutableEntry<Any, V>> {

        private var pos = -1
        private var lastPos: Int = 0

        init {
            goNext()
        }

        private fun goNext() {
            lastPos = pos
            pos++
            while (pos < _keys.size && _keys[pos] == null) pos++
        }

        /**
         * Returns `true` if there are more entries to iterate.
         *
         * ## Behavior / Contract
         * - Returns `true` while the internal pointer points to a valid slot within the array bounds.
         * - Does not mutate state.
         *
         * ```kotlin
         * val map = CharArrayMap<Int>(2).apply { put("a", 1) }
         * // map.EntryIterator(true).hasNext() == true
         * ```
         */
        override fun hasNext(): Boolean {
            return pos < _keys.size
        }

        /**
         * Returns the raw `CharArray` reference for the next key.
         *
         * ## Behavior / Contract
         * - Returns the internal array reference directly; external modification may affect map behaviour.
         *
         * ```kotlin
         * val map = CharArrayMap<Int>(2).apply { put("a", 1) }
         * val key = map.EntryIterator(true).nextKey()
         * // String(key).isNotEmpty() == true
         * ```
         */
        fun nextKey(): CharArray {
            goNext()
            return _keys[lastPos].requireNotNull("_keys[lastPos=$lastPos]")
        }

        /**
         * Returns the next key as a new [String] copy.
         *
         * ## Behavior / Contract
         * - Converts the result of [nextKey] into a new String instance.
         * - Safe to use when the caller must not hold a reference to the internal key array.
         *
         * ```kotlin
         * val map = CharArrayMap<Int>(2).apply { put("a", 1) }
         * val key = map.EntryIterator(true).nextKeyString()
         * // key == "a"
         * ```
         */
        fun nextKeyString(): String {
            return String(nextKey())
        }

        /**
         * Returns the value associated with the most recently returned key.
         *
         * ## Behavior / Contract
         * - Based on the position set by the last [nextKey] or [next] call.
         * - May return `null` if the slot has no value.
         *
         * ```kotlin
         * val map = CharArrayMap<Int>(2).apply { put("a", 1) }
         * val it = map.EntryIterator(true)
         * it.nextKey()
         * // it.currentValue() == 1
         * ```
         */
        fun currentValue(): V? {
            return _values[lastPos]
        }

        /**
         * Replaces the value at the most recently returned key position.
         *
         * ## Behavior / Contract
         * - Throws [UnsupportedOperationException] when `allowModify` is `false`.
         * - Returns the previous value on success.
         *
         * ```kotlin
         * val map = CharArrayMap<Int>(2).apply { put("a", 1) }
         * val it = map.EntryIterator(true)
         * it.nextKey()
         * // it.setValue(2) == 1
         * ```
         */
        fun setValue(value: V): V? {
            if (!allowModify)
                throw UnsupportedOperationException()
            val old = _values[lastPos]
            _values[lastPos] = value
            return old
        }

        /**
         * Returns the current entry as a [MutableMap.MutableEntry] and advances the iterator.
         *
         * ## Behavior / Contract
         * - Moves the internal pointer to the next valid slot.
         * - Whether the returned entry supports [MutableMap.MutableEntry.setValue] depends on `allowModify`.
         *
         * ```kotlin
         * val map = CharArrayMap<Int>(2).apply { put("a", 1) }
         * val entry = map.EntryIterator(true).next()
         * // entry.value == 1
         * ```
         */
        override fun next(): MutableMap.MutableEntry<Any, V> {
            goNext()
            return MapEntry(lastPos, allowModify)
        }

        /**
         * Iterator-based removal is not supported.
         *
         * ## Behavior / Contract
         * - Always throws [UnsupportedOperationException].
         * - Use the map-level `remove(key)` API instead.
         *
         * ```kotlin
         * val map = CharArrayMap<Int>(2).apply { put("a", 1) }
         * // map.EntryIterator(true).remove() throws UnsupportedOperationException
         * ```
         */
        override fun remove() {
            throw UnsupportedOperationException()
        }
    }

    private inner class MapEntry(
        private val pos: Int,
        private val allowModify: Boolean,
    ): MutableMap.MutableEntry<Any, V> {
        override val key: Any
            get() = _keys[pos].requireNotNull("_keys[pos=$pos]").clone()

        override val value: V
            get() = _values[pos].requireNotNull("_values[pos=$pos]")

        override fun setValue(newValue: V): V {
            if (!allowModify)
                throw UnsupportedOperationException()

            val old = _values[pos]
            _values[pos] = newValue
            return old.requireNotNull("_values[pos=$pos]")
        }

        override fun toString(): String {
            return String(_keys[pos].requireNotNull("_keys[pos=$pos]")) + '=' +
                    if (_values[pos] === this@CharArrayMap) "(this Map)" else _values[pos]
        }
    }

    /**
     * Entry set view implementation for this map.
     *
     * ## Behavior / Contract
     * - Iteration is performed via [EntryIterator].
     * - When `allowModify` is `false`, mutating operations including [clear] throw [UnsupportedOperationException].
     * - [size] reflects the enclosing map's `_count` directly.
     *
     * ```kotlin
     * val map = CharArrayMap<Int>(2).apply { put("a", 1) }
     * // map.EntrySet(true).size == 1
     * ```
     */
    inner class EntrySet(private val allowModify: Boolean): AbstractMutableSet<MutableMap.MutableEntry<Any, V>>() {

        /**
         * Returns an iterator over the entries in this set.
         *
         * ## Behavior / Contract
         * - Creates an [EntryIterator] that shares the `allowModify` setting.
         * - Iterator-level `remove` is not supported.
         *
         * ```kotlin
         * val map = CharArrayMap<Int>(2).apply { put("a", 1) }
         * // map.EntrySet(true).iterator().hasNext() == true
         * ```
         */
        override fun iterator(): MutableIterator<MutableMap.MutableEntry<Any, V>> {
            return EntryIterator(allowModify)
        }

        /**
         * Returns `true` if [element] is present in the map with an equal value.
         *
         * ## Behavior / Contract
         * - Looks up the current value by key and compares it with the entry's value.
         * - Returns `false` when the key is absent.
         *
         * ```kotlin
         * val map = CharArrayMap<Int>(2).apply { put("a", 1) }
         * val entry = map.entries.first()
         * // map.EntrySet(true).contains(entry) == true
         * ```
         */
        override fun contains(element: MutableMap.MutableEntry<Any, V>): Boolean {
            val key = element.key
            val value = element.value
            val v = get(key)
            return if (v == null) value == null else v == value
        }

        /**
         * Direct entry addition is not supported.
         *
         * ## Behavior / Contract
         * - Always throws [UnsupportedOperationException].
         * - Use the map-level `put` API to add entries.
         *
         * ```kotlin
         * val map = CharArrayMap<Int>(2)
         * // map.EntrySet(true).add(map.entries.first()) throws UnsupportedOperationException
         * ```
         */
        override fun add(element: MutableMap.MutableEntry<Any, V>): Boolean {
            throw UnsupportedOperationException()
        }

        /**
         * Direct entry removal is not supported.
         *
         * ## Behavior / Contract
         * - Always throws [UnsupportedOperationException].
         * - Use the map-level `remove(key)` API instead.
         *
         * ```kotlin
         * val map = CharArrayMap<Int>(2).apply { put("a", 1) }
         * // map.EntrySet(true).remove(map.entries.first()) throws UnsupportedOperationException
         * ```
         */
        override fun remove(element: MutableMap.MutableEntry<Any, V>): Boolean {
            throw UnsupportedOperationException()
        }

        /**
         * Returns the number of entries in this set.
         *
         * ## Behavior / Contract
         * - Directly reflects the enclosing map's `_count`.
         * - Read-only; does not mutate state.
         *
         * ```kotlin
         * val map = CharArrayMap<Int>(2).apply { put("a", 1) }
         * // map.EntrySet(true).size == 1
         * ```
         */
        override val size: Int
            get() = _count

        /**
         * Clears all entries from this set.
         *
         * ## Behavior / Contract
         * - Throws [UnsupportedOperationException] when `allowModify` is `false`.
         * - Delegates to the enclosing map's [clear] when permitted.
         *
         * ```kotlin
         * val map = CharArrayMap<Int>(2).apply { put("a", 1) }
         * map.EntrySet(true).clear()
         * // map.isEmpty() == true
         * ```
         */
        override fun clear() {
            if (!allowModify) throw UnsupportedOperationException()
            this@CharArrayMap.clear()
        }
    }

    /**
     * Read-only [CharArrayMap] wrapper that blocks all mutation operations.
     *
     * ## Behavior / Contract
     * - [put], [remove], and [clear] throw [UnsupportedOperationException].
     * - Read operations ([get], [containsKey]) reflect the underlying backing arrays.
     *
     * ## Aliasing
     * This class shares backing arrays with the source map (shallow copy), following the same
     * contract as [java.util.Collections.unmodifiableMap]. Callers must not retain a mutable
     * reference to the original after wrapping. Use [unmodifiableMap] rather than constructing
     * this class directly.
     *
     * ```kotlin
     * val readonly = CharArrayMap.unmodifiableMap(CharArrayMap<Int>(2).apply { put("a", 1) })
     * // readonly["a"] == 1
     * // readonly.put("b", 2) throws UnsupportedOperationException
     * ```
     */
    open class UnmodifiableCharArrayMap<V>(map: CharArrayMap<V>): CharArrayMap<V>(map) {

        override fun clear() = throw UnsupportedOperationException()

        override fun put(key: Any, value: V): V? = throw UnsupportedOperationException()
        override fun put(text: CharArray, value: V): V? = throw UnsupportedOperationException()
        override fun put(text: CharSequence, value: V): V? = throw UnsupportedOperationException()
        override fun put(text: String, value: V): V? = throw UnsupportedOperationException()

        override fun remove(key: Any, value: V): Boolean = throw UnsupportedOperationException()
        override fun remove(key: Any): V? = throw UnsupportedOperationException()

        override fun createEntrySet(): EntrySet = EntrySet(false)
    }

    private class EmptyCharArrayMap<V>: UnmodifiableCharArrayMap<V>(CharArrayMap(0)) {

        override fun containsKey(text: CharArray, off: Int, len: Int): Boolean = false
        override fun containsKey(cs: CharSequence): Boolean = false
        override fun containsKey(key: Any): Boolean = false
        override fun get(text: CharArray, off: Int, len: Int): V? = null
        override fun get(cs: CharSequence): V? = null
        override fun get(key: Any): V? = null
    }

}
