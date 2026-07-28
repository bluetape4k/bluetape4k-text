package io.bluetape4k.tokenizer.utils

import io.bluetape4k.logging.KLogging
import java.io.Serializable

/**
 * Char-array 기반 key를 중복 없이 저장하는 mutable set입니다.
 *
 * ## 동작 계약
 * - [CharArrayMap]을 backing store로 사용하며 entry마다 placeholder value만 저장합니다.
 * - `Any`, `String`, `CharSequence`, `CharArray` input을 받습니다.
 * - Word dictionary와 block-word list처럼 membership lookup이 잦은 용도에 맞춰 최적화합니다.
 *
 * @property map set entry를 저장하는 내부 [CharArrayMap]입니다. Key 비교와 저장 정책을 이 map에 위임합니다.
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
         * [set]의 read-only view를 반환합니다.
         *
         * ## 동작 계약
         * - Empty set은 공유 `EMPTY_SET` singleton을 반환합니다.
         * - 그 외 set은 [CharArrayMap.UnmodifiableCharArrayMap] 기반 instance로 감쌉니다.
         *
         * @param set read-only view로 감쌀 source set입니다.
         * @return mutation을 허용하지 않는 [CharArraySet] view입니다.
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
         * [set]의 내용을 복사해 새 [CharArraySet]을 만듭니다.
         *
         * ## 동작 계약
         * - [set]이 [CharArraySet]이면 internal map copy constructor를 사용합니다.
         * - [set]이 공유 empty singleton이면 같은 singleton을 반환합니다.
         *
         * @param set 복사할 source set입니다.
         * @return source 내용을 담은 새 [CharArraySet]입니다. Empty singleton은 그대로 반환할 수 있습니다.
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
     * 예상 element 수를 기준으로 initial capacity hint를 가진 set을 만듭니다.
     *
     * ## 동작 계약
     * - 내부적으로 `CharArrayMap(startSize)`를 만듭니다.
     * - Capacity는 rehashing을 늦추기 위한 hint입니다.
     *
     * @param startSize 예상 element 수에 맞춘 초기 map size hint입니다.
     *
     * ```kotlin
     * val set = CharArraySet(128)
     * // set.isEmpty() == true
     * ```
     */
    constructor(startSize: Int): this(CharArrayMap<Any>(startSize))

    /**
     * [c]의 내용을 복사해 set을 만듭니다.
     *
     * ## 동작 계약
     * - `c.size`를 initial capacity로 사용한 뒤 `addAll`로 채웁니다.
     * - Duplicate element는 set semantic에 따라 deduplicate됩니다.
     *
     * @param c 새 set에 복사할 source collection입니다.
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
     * Set의 모든 element를 제거합니다.
     *
     * ## 동작 계약
     * - [CharArrayMap.clear]에 위임해 모든 key/value slot을 reset합니다.
     * - 호출 후 [size]는 0입니다.
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
     * [element]가 set에 있으면 `true`를 반환합니다.
     *
     * ## 동작 계약
     * - 모든 `Any` input에 대해 internal map의 key-comparison rule에 위임합니다.
     *
     * @param element 존재 여부를 확인할 element입니다.
     * @return 같은 문자 content를 가진 key가 있으면 `true`, 없으면 `false`입니다.
     *
     * ```kotlin
     * val set = CharArraySet(2).apply { add("hello") }
     * // set.contains("hello") == true
     * ```
     */
    override fun contains(element: Any): Boolean = map.containsKey(element)

    /**
     * Char-array slice `text[off, off+len)`이 set에 있으면 `true`를 반환합니다.
     *
     * ## 동작 계약
     * - 주어진 range에 대한 slot search를 internal map에 위임합니다.
     *
     * @param text 검색할 source char array입니다.
     * @param off 검색을 시작할 offset입니다.
     * @param len 검색할 char 개수입니다. 기본값은 [text] 전체 size입니다.
     * @return 같은 char-array slice가 key로 있으면 `true`, 없으면 `false`입니다.
     *
     * ```kotlin
     * val set = CharArraySet(4).apply { add("token") }
     * val chars = "token".toCharArray()
     * // set.contains(chars, 0, chars.size) == true
     * ```
     */
    fun contains(text: CharArray, off: Int, len: Int = text.size) = map.containsKey(text, off, len)

    /**
     * [cs]가 set에 있으면 `true`를 반환합니다.
     *
     * ## 동작 계약
     * - Case normalization 없이 raw character content로 비교합니다.
     *
     * @param cs 존재 여부를 확인할 character sequence입니다.
     * @return 같은 문자 content를 가진 key가 있으면 `true`, 없으면 `false`입니다.
     *
     * ```kotlin
     * val set = CharArraySet(2).apply { add("hello") }
     * // set.contains("hello") == true
     * ```
     */
    fun contains(cs: CharSequence) = map.containsKey(cs)

    /**
     * [element]를 set에 추가합니다.
     *
     * ## 동작 계약
     * - Element 등록을 위해 internal map에 placeholder value를 저장합니다.
     * - Key가 아직 없었을 때만 `true`를 반환합니다.
     *
     * @param element 추가할 element입니다.
     * @return 새 key가 추가되면 `true`, 이미 있으면 `false`입니다.
     *
     * ```kotlin
     * val set = CharArraySet(2)
     * // set.add("x") == true
     * ```
     */
    override fun add(element: Any): Boolean = map.put(element, PLACEHOLDER) == null

    /**
     * [CharSequence] key를 set에 추가합니다.
     *
     * ## 동작 계약
     * - Deduplication은 character content 기준으로 수행합니다.
     * - Key가 이미 있으면 `false`를 반환합니다.
     *
     * @param text 추가할 character sequence key입니다.
     * @return 새 key가 추가되면 `true`, 이미 있으면 `false`입니다.
     *
     * ```kotlin
     * val set = CharArraySet(2)
     * // set.add("abc" as CharSequence) == true
     * ```
     */
    open fun add(text: CharSequence) = map.put(text, PLACEHOLDER) == null

    /**
     * [String] key를 set에 추가합니다.
     *
     * ## 동작 계약
     * - Internal map의 string insert path를 사용합니다.
     * - Duplicate key는 삽입하지 않습니다.
     *
     * @param text 추가할 string key입니다.
     * @return 새 key가 추가되면 `true`, 이미 있으면 `false`입니다.
     *
     * ```kotlin
     * val set = CharArraySet(2)
     * // set.add("abc") == true
     * ```
     */
    open fun add(text: String) = map.put(text, PLACEHOLDER) == null

    /**
     * Char-array key를 set에 추가합니다.
     *
     * ## 동작 계약
     * - Array reference를 internal key로 직접 저장합니다.
     * - 같은 char sequence가 이미 있으면 `false`를 반환합니다.
     *
     * @param text 추가할 char-array key입니다.
     * @return 새 key가 추가되면 `true`, 이미 있으면 `false`입니다.
     *
     * ```kotlin
     * val set = CharArraySet(2)
     * // set.add("abc".toCharArray()) == true
     * ```
     */
    open fun add(text: CharArray) = map.put(text, PLACEHOLDER) == null

    /**
     * [elements]의 모든 element를 set에 추가합니다.
     *
     * ## 동작 계약
     * - 각 element를 순회하며 [add]를 호출합니다.
     * - 새 element가 하나 이상 추가되면 `true`를 반환합니다.
     *
     * @param elements 추가할 element collection입니다.
     * @return 하나 이상 새로 추가되면 `true`, 모두 이미 있으면 `false`입니다.
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
     * [element]를 set에서 제거합니다.
     *
     * ## 동작 계약
     * - Element가 실제로 있었고 제거됐을 때만 `true`를 반환합니다.
     * - Element가 없으면 exception 없이 `false`를 반환합니다.
     *
     * @param element 제거할 element입니다.
     * @return 제거에 성공하면 `true`, 대상이 없으면 `false`입니다.
     *
     * ```kotlin
     * val set = CharArraySet(2).apply { add("a") }
     * // set.remove("a") == true
     * ```
     */
    override fun remove(element: Any): Boolean = map.remove(element) != null

    /**
     * [String] key를 set에서 제거합니다.
     *
     * ## 동작 계약
     * - Key가 실제로 있었고 제거됐을 때만 `true`를 반환합니다.
     * - Key가 없으면 `false`를 반환합니다.
     *
     * @param text 제거할 string key입니다.
     * @return 제거에 성공하면 `true`, 대상이 없으면 `false`입니다.
     *
     * ```kotlin
     * val set = CharArraySet(2).apply { add("x") }
     * set.remove("x")
     * // set.contains("x") == false
     * ```
     */
    fun remove(text: String): Boolean = map.remove(text) != null

    /**
     * [elements]의 모든 element를 set에서 제거합니다.
     *
     * ## 동작 계약
     * - 각 element에 대해 [remove]를 호출합니다.
     * - 하나 이상 실제로 제거되면 `true`를 반환합니다.
     *
     * @param elements 제거할 element collection입니다.
     * @return 하나 이상 제거되면 `true`, 제거된 항목이 없으면 `false`입니다.
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
     * [words]의 모든 string을 set에서 제거합니다.
     *
     * ## 동작 계약
     * - List 안의 각 string에 대해 [remove]를 호출합니다.
     * - 하나 이상 실제로 제거되면 `true`를 반환합니다.
     *
     * @param words 제거할 string key 목록입니다.
     * @return 하나 이상 제거되면 `true`, 제거된 항목이 없으면 `false`입니다.
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
     * Set에 들어 있는 element 수를 반환합니다.
     *
     * ## 동작 계약
     * - Internal map의 [size]에 직접 위임합니다.
     * - Read-only이며 state를 mutate하지 않습니다.
     *
     * ```kotlin
     * val set = CharArraySet(2).apply { add("one") }
     * // set.size == 1
     * ```
     */
    override val size: Int
        get() = map.size

    /**
     * Set element를 순회하는 iterator를 반환합니다.
     *
     * ## 동작 계약
     * - Internal map의 original key set iterator를 재사용합니다.
     * - 반환되는 element는 raw `CharArray` key reference입니다.
     *
     * @return set element를 순회하는 mutable iterator입니다.
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
