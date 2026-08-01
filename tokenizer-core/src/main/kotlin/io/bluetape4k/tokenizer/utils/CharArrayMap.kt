package io.bluetape4k.tokenizer.utils

import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.requireNotNull
import java.io.Serializable

/**
 * `CharArray` key에 맞춰 최적화한 open-addressing hash map입니다.
 *
 * ## 동작 계약
 * - Key type은 `Any`를 받지만 내부에서는 `CharArray` 또는 string representation으로 normalize합니다.
 * - Collision resolution은 linear probing 대신 increment 기반(`inc`) probe strategy를 사용합니다.
 * - Remove 시 probe chain을 보존하기 위해 남은 entry를 다시 insert합니다.
 *
 * @param V map에 저장할 value type입니다.
 * @param startSize 예상 entry 수에 맞춘 initial capacity hint입니다.
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
        private const val serialVersionUID: Long = 1L
        private const val INIT_SIZE = 8
        private val EMPTY_MAP: CharArrayMap<Any> = EmptyCharArrayMap()

        @JvmStatic
        /**
         * Mutation operation을 모두 막는 read-only view를 반환합니다.
         *
         * ## 동작 계약
         * - Empty map은 공유 [emptyMap] singleton을 반환합니다.
         * - 이미 [UnmodifiableCharArrayMap]인 map은 그대로 반환합니다.
         * - 그 외 map은 [UnmodifiableCharArrayMap]으로 감쌉니다.
         *
         * ## Aliasing 경고
         * 반환된 view는 [java.util.Collections.unmodifiableMap]처럼 원본 map의 backing array를 공유합니다.
         * Caller가 원본 mutable map reference를 보관한 채 계속 write하면 그 변경이 unmodifiable view에도
         * 보입니다. 이를 막으려면 `unmodifiableMap` 호출 뒤 원본 reference를 버리세요.
         *
         * @param map read-only view로 감쌀 source map입니다.
         * @return mutation을 허용하지 않는 [CharArrayMap] view입니다.
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
         * Standard [Map]을 새 [CharArrayMap]으로 복사합니다.
         *
         * ## 동작 계약
         * - Source map의 모든 entry를 `putAll`로 load합니다.
         * - Source map과 result는 서로 독립된 instance입니다.
         *
         * @param map 복사할 source map입니다.
         * @return source entry를 담은 새 [CharArrayMap]입니다.
         *
         * ```kotlin
         * val copied = CharArrayMap.copy(mapOf<Any, Int>("x" to 10))
         * // copied["x"] == 10
         * ```
         */
        fun <V> copy(map: Map<Any, V>): CharArrayMap<V> = CharArrayMap(map)

        @JvmStatic
        /**
         * 공유 immutable empty-map singleton을 반환합니다.
         *
         * ## 동작 계약
         * - 같은 internal `EMPTY_MAP` instance를 요청 type으로 cast해 반환합니다.
         * - Insert와 remove operation은 지원하지 않습니다.
         *
         * @return 요청한 value type으로 cast된 empty [CharArrayMap] singleton입니다.
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
     * [c]의 모든 entry를 복사해 map을 만듭니다.
     *
     * ## 동작 계약
     * - Initial capacity는 `c.size`에서 계산합니다.
     * - 모든 entry는 construction time에 `putAll(c)`로 load합니다.
     *
     * @param c 새 map에 복사할 source map입니다.
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
     * [src]의 backing array를 공유하는 shallow copy를 만듭니다.
     *
     * ## 동작 계약
     * - `_keys`와 `_values` array reference를 직접 공유합니다.
     * - 두 map이 모두 mutable 상태라면 한쪽 mutation이 다른 쪽에도 보입니다.
     *
     * @param src backing array를 공유할 source [CharArrayMap]입니다.
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
     * Map의 모든 entry를 제거합니다.
     *
     * ## 동작 계약
     * - Key array와 value array의 모든 slot을 `null`로 설정합니다.
     * - `_count`를 0으로 reset합니다.
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
     * Char-array slice `text[off, off+len)`이 key로 존재하는지 반환합니다.
     *
     * ## 동작 계약
     * - 주어진 range의 hash slot을 계산하고 non-null entry가 있는지 확인합니다.
     *
     * @param text 검색할 source char array입니다.
     * @param off 검색을 시작할 offset입니다.
     * @param len 검색할 char 개수입니다.
     * @return 같은 slice key가 있으면 `true`, 없으면 `false`입니다.
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
     * [cs]가 key로 존재하는지 반환합니다.
     *
     * ## 동작 계약
     * - [cs]의 character content를 기준으로 slot을 계산합니다.
     *
     * @param cs 검색할 character sequence key입니다.
     * @return 같은 문자 content의 key가 있으면 `true`, 없으면 `false`입니다.
     *
     * ```kotlin
     * val map = CharArrayMap<Int>(2).apply { put("key", 1) }
     * // map.containsKey("key") == true
     * ```
     */
    open fun containsKey(cs: CharSequence): Boolean = _keys[getSlot(cs)] != null

    /**
     * [key]가 map에 존재하는지 반환합니다.
     *
     * ## 동작 계약
     * - `CharArray` key는 array lookup path를 사용합니다.
     * - 다른 type은 lookup에 `toString()`을 사용합니다.
     *
     * @param key 검색할 key입니다.
     * @return key가 있으면 `true`, 없으면 `false`입니다.
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
     * Char-array slice `text[off, off+len)`에 연결된 value를 반환하며, 없으면 `null`을 반환합니다.
     *
     * ## 동작 계약
     * - Key comparison은 character 단위로 수행합니다.
     *
     * @param text 검색할 source char array입니다.
     * @param off 검색을 시작할 offset입니다.
     * @param len 검색할 char 개수입니다.
     * @return 연결된 value입니다. Key가 없으면 `null`입니다.
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
     * [cs]에 연결된 value를 반환하며, 없으면 `null`을 반환합니다.
     *
     * ## 동작 계약
     * - Slot lookup은 [cs]의 string content를 사용해 수행합니다.
     *
     * @param cs 검색할 character sequence key입니다.
     * @return 연결된 value입니다. Key가 없으면 `null`입니다.
     *
     * ```kotlin
     * val map = CharArrayMap<Int>(2).apply { put("y", 7) }
     * // map.get("y") == 7
     * ```
     */
    open fun get(cs: CharSequence): V? = _values[getSlot(cs)]

    /**
     * [key]에 연결된 value를 반환하며, 없으면 `null`을 반환합니다.
     *
     * ## 동작 계약
     * - `CharArray` key는 array path를 사용하고, 다른 type은 `toString()`을 사용합니다.
     *
     * @param key 검색할 key입니다.
     * @return 연결된 value입니다. Key가 없으면 `null`입니다.
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
     * [CharSequence] key와 연결 value를 저장합니다.
     *
     * ## 동작 계약
     * - Key를 String으로 변환하고 String put path에 위임합니다.
     * - Key가 이미 있으면 previous value를 반환합니다.
     *
     * @param text 저장할 character sequence key입니다.
     * @param value key와 연결할 value입니다.
     * @return 이전 value입니다. 새 key이면 `null`입니다.
     *
     * ```kotlin
     * val map = CharArrayMap<Int>(2)
     * map.put("z", 1)
     * // map.put("z", 2) == 1
     * ```
     */
    open fun put(text: CharSequence, value: V): V? = put(text.toString(), value)

    /**
     * 임의의 key와 연결 value를 저장합니다.
     *
     * ## 동작 계약
     * - `CharArray` key는 array insert path를 사용하고, 다른 type은 `toString()`을 사용합니다.
     *
     * @param key 저장할 key입니다.
     * @param value key와 연결할 value입니다.
     * @return 이전 value입니다. 새 key이면 `null`입니다.
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
     * String key와 연결 value를 저장합니다.
     *
     * ## 동작 계약
     * - Key를 `CharArray`로 변환하고 array put path에 위임합니다.
     * - Key가 이미 있으면 previous value를 반환합니다.
     *
     * @param text 저장할 string key입니다.
     * @param value key와 연결할 value입니다.
     * @return 이전 value입니다. 새 key이면 `null`입니다.
     *
     * ```kotlin
     * val map = CharArrayMap<Int>(2)
     * // map.put("k", 9) == null
     * ```
     */
    open fun put(text: String, value: V): V? = put(text.toCharArray(), value)

    /**
     * Char-array key와 연결 value를 저장합니다.
     *
     * ## 동작 계약
     * - Key가 이미 있으면 existing value를 교체하고 old value를 반환합니다.
     * - 새 key이면 `_count`를 증가시키고 load factor(~0.8)를 넘을 때 `rehash`를 실행합니다.
     * - Key array는 reference로 저장하므로 insert 후 external mutation이 map behavior에 영향을 줍니다.
     *
     * @param text 저장할 char-array key입니다.
     * @param value key와 연결할 value입니다.
     * @return 이전 value입니다. 새 key이면 `null`입니다.
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
                // Rehash 중이다. Key는 unique하지만 getSlot은 여전히 char comparison으로 empty slot을 probe한다.
                // Collision 없는 probe(null만 비교)가 더 빠르겠지만 향후 최적화로 남긴다.
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
     * [key]를 map에서 제거하고 previous value를 반환합니다. Key가 없으면 `null`을 반환합니다.
     *
     * ## 동작 계약
     * - Key가 있으면 probe chain을 rebuild하기 위해 남은 모든 entry를 다시 insert합니다.
     * - 제거에 성공하면 removed value를 반환합니다.
     *
     * @param key 제거할 key입니다.
     * @return 제거한 value입니다. Key가 없으면 `null`입니다.
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
     * 현재 map에 저장된 entry 수입니다.
     *
     * ## 동작 계약
     * - Insert 시 증가하고 `clear` / `remove` 시 감소하거나 다시 계산됩니다.
     * - Internal `_count` counter를 직접 반영합니다.
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
     * Entry set view 생성을 담당하는 확장 지점입니다.
     *
     * ## 동작 계약
     * - 기본 implementation은 mutable `EntrySet(true)`를 반환합니다.
     * - Read-only subclass는 unmodifiable set을 반환하도록 이 함수를 override해야 합니다.
     *
     * @return 이 map의 entry view입니다.
     *
     * ```kotlin
     * val map = CharArrayMap<Int>(2)
     * // map.entries.isEmpty() == true
     * ```
     */
    protected open fun createEntrySet(): EntrySet = EntrySet(true)

    /**
     * 이 map의 entry set view를 반환합니다.
     *
     * ## 동작 계약
     * - Lazy initialized `_entrySet` instance를 재사용합니다.
     * - 반환된 view의 mutability는 [createEntrySet] implementation에 따라 달라집니다.
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
     * Raw `CharArray` key reference를 copy 없이 노출하는 read-only key set입니다.
     *
     * ## 동작 계약
     * - Iteration은 internal `_keys` array slot에 대한 direct reference를 반환합니다.
     * - Add와 remove operation은 지원하지 않습니다.
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
     * Standard `Map.keys` view를 반환합니다.
     *
     * ## 동작 계약
     * - Key addition은 허용하지 않으며 lookup 용도로만 사용합니다.
     * - 내부적으로 `CharArraySet` wrapper가 backing합니다.
     *
     * ```kotlin
     * val map = CharArrayMap<Int>(2).apply { put("k", 3) }
     * // map.keys.contains("k") == true
     * ```
     */
    override val keys: MutableSet<Any> get() = _keySet

    /**
     * Internal slot을 순회하며 map entry를 만드는 iterator입니다.
     *
     * ## 동작 계약
     * - `allowModify`가 `false`이면 [setValue]는 [UnsupportedOperationException]을 던집니다.
     * - [nextKey]는 raw internal `CharArray` reference를 반환합니다.
     * - [remove]는 지원하지 않습니다.
     *
     * @param allowModify `true`이면 iterator entry value 변경을 허용합니다.
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
         * 순회할 entry가 더 있으면 `true`를 반환합니다.
         *
         * ## 동작 계약
         * - Internal pointer가 array bounds 안의 valid slot을 가리키는 동안 `true`를 반환합니다.
         * - State를 mutate하지 않습니다.
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
         * 다음 key의 raw `CharArray` reference를 반환합니다.
         *
         * ## 동작 계약
         * - Internal array reference를 직접 반환하므로 external modification이 map behavior에 영향을 줄 수 있습니다.
         *
         * @return 다음 key의 internal `CharArray` reference입니다.
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
         * 다음 key를 새 [String] copy로 반환합니다.
         *
         * ## 동작 계약
         * - [nextKey] 결과를 새 String instance로 변환합니다.
         * - Caller가 internal key array reference를 보관하면 안 되는 경우 안전하게 사용할 수 있습니다.
         *
         * @return 다음 key의 string copy입니다.
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
         * 가장 최근 반환한 key에 연결된 value를 반환합니다.
         *
         * ## 동작 계약
         * - 마지막 [nextKey] 또는 [next] call이 설정한 position을 기준으로 합니다.
         * - Slot에 value가 없으면 `null`을 반환할 수 있습니다.
         *
         * @return 현재 iterator position의 value입니다.
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
         * 가장 최근 반환한 key position의 value를 교체합니다.
         *
         * ## 동작 계약
         * - `allowModify`가 `false`이면 [UnsupportedOperationException]을 던집니다.
         * - 성공하면 previous value를 반환합니다.
         *
         * @param value 새로 저장할 value입니다.
         * @return 교체 전 value입니다.
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
         * Current entry를 [MutableMap.MutableEntry]로 반환하고 iterator를 전진시킵니다.
         *
         * ## 동작 계약
         * - Internal pointer를 다음 valid slot으로 이동합니다.
         * - 반환된 entry가 [MutableMap.MutableEntry.setValue]를 지원하는지는 `allowModify`에 따라 달라집니다.
         *
         * @return 현재 slot의 mutable map entry view입니다.
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
         * Iterator 기반 제거는 지원하지 않습니다.
         *
         * ## 동작 계약
         * - 항상 [UnsupportedOperationException]을 던집니다.
         * - 대신 map-level `remove(key)` API를 사용합니다.
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
     * 이 map의 entry set view implementation입니다.
     *
     * ## 동작 계약
     * - Iteration은 [EntryIterator]로 수행합니다.
     * - `allowModify`가 `false`이면 [clear]를 포함한 mutating operation이 [UnsupportedOperationException]을 던집니다.
     * - [size]는 enclosing map의 `_count`를 직접 반영합니다.
     *
     * @param allowModify `true`이면 view를 통한 clear와 entry value 변경을 허용합니다.
     *
     * ```kotlin
     * val map = CharArrayMap<Int>(2).apply { put("a", 1) }
     * // map.EntrySet(true).size == 1
     * ```
     */
    inner class EntrySet(private val allowModify: Boolean): AbstractMutableSet<MutableMap.MutableEntry<Any, V>>() {

        /**
         * 이 set의 entry를 순회하는 iterator를 반환합니다.
         *
         * ## 동작 계약
         * - `allowModify` setting을 공유하는 [EntryIterator]를 만듭니다.
         * - Iterator-level `remove`는 지원하지 않습니다.
         *
         * @return entry를 순회하는 [EntryIterator]입니다.
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
         * [element]와 같은 key/value entry가 map에 있으면 `true`를 반환합니다.
         *
         * ## 동작 계약
         * - Key로 current value를 lookup한 뒤 entry value와 비교합니다.
         * - Key가 없으면 `false`를 반환합니다.
         *
         * @param element 존재 여부를 확인할 mutable map entry입니다.
         * @return 같은 key/value entry가 있으면 `true`, 없으면 `false`입니다.
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
         * 직접 entry 추가는 지원하지 않습니다.
         *
         * ## 동작 계약
         * - 항상 [UnsupportedOperationException]을 던집니다.
         * - Entry를 추가하려면 map-level `put` API를 사용합니다.
         *
         * @param element 추가하려 한 entry입니다. 이 API에서는 항상 거부됩니다.
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
         * 직접 entry 제거는 지원하지 않습니다.
         *
         * ## 동작 계약
         * - 항상 [UnsupportedOperationException]을 던집니다.
         * - 대신 map-level `remove(key)` API를 사용합니다.
         *
         * @param element 제거하려 한 entry입니다. 이 API에서는 항상 거부됩니다.
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
         * 이 set의 entry 수를 반환합니다.
         *
         * ## 동작 계약
         * - Enclosing map의 `_count`를 직접 반영합니다.
         * - Read-only이며 state를 mutate하지 않습니다.
         *
         * ```kotlin
         * val map = CharArrayMap<Int>(2).apply { put("a", 1) }
         * // map.EntrySet(true).size == 1
         * ```
         */
        override val size: Int
            get() = _count

        /**
         * 이 set의 모든 entry를 clear합니다.
         *
         * ## 동작 계약
         * - `allowModify`가 `false`이면 [UnsupportedOperationException]을 던집니다.
         * - 허용되는 경우 enclosing map의 [clear]에 위임합니다.
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
     * 모든 mutation operation을 막는 read-only [CharArrayMap] wrapper입니다.
     *
     * ## 동작 계약
     * - [put], [remove], [clear]는 [UnsupportedOperationException]을 던집니다.
     * - Read operation([get], [containsKey])은 underlying backing array를 반영합니다.
     *
     * ## Aliasing
     * 이 class는 [java.util.Collections.unmodifiableMap]과 같은 계약으로 source map과 backing array를
     * 공유합니다(shallow copy). Caller는 wrapping 후 원본 mutable reference를 보관하지 않아야 합니다.
     * 이 class를 직접 만들기보다 [unmodifiableMap]을 사용하세요.
     *
     * @param map read-only wrapper가 backing array를 공유할 source map입니다.
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
