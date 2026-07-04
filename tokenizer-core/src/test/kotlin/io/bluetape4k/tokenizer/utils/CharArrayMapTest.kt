package io.bluetape4k.tokenizer.utils

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldHaveSize
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.logging.KLogging
import org.junit.jupiter.api.Test

/**
 * CharArrayMap 테스트
 */
class CharArrayMapTest {
    companion object: KLogging()

    @Test
    fun `create empty CharArrayMap`() {
        val map = CharArrayMap<String>(16)
        map shouldHaveSize 0
        map.isEmpty().shouldBeTrue()
    }

    @Test
    fun `put and get values`() {
        val map = CharArrayMap<String>(16)

        map["hello"] = "world"
        map["test"] = "value"

        map shouldHaveSize 2
        map["hello"] shouldBeEqualTo "world"
        map["test"] shouldBeEqualTo "value"
    }

    @Test
    fun `put with CharArray`() {
        val map = CharArrayMap<String>(16)
        val key = "test".toCharArray()

        map[key] = "value"

        map[key] shouldBeEqualTo "value"
        map["test"] shouldBeEqualTo "value"
    }

    @Test
    fun `get with CharArray and offset`() {
        val map = CharArrayMap<String>(16)
        map["hello"] = "value1"
        map["world"] = "value2"

        val text = "hello world".toCharArray()
        map.get(text, 0, 5) shouldBeEqualTo "value1"
        map.get(text, 6, 5) shouldBeEqualTo "value2"
    }

    @Test
    fun `update existing key`() {
        val map = CharArrayMap<String>(16)

        map["key"] = "value1"
        map["key"] = "value2"

        map shouldHaveSize 1
        map["key"] shouldBeEqualTo "value2"
    }

    @Test
    fun `containsKey with String`() {
        val map = CharArrayMap<String>(16)
        map["test"] = "value"

        map.containsKey("test").shouldBeTrue()
        map.containsKey("notexist").shouldBeFalse()
    }

    @Test
    fun `containsKey with CharArray`() {
        val map = CharArrayMap<String>(16)
        map["test"] = "value"

        map.containsKey("test".toCharArray()).shouldBeTrue()
        map.containsKey("other".toCharArray()).shouldBeFalse()
    }

    @Test
    fun `containsKey with offset and length`() {
        val map = CharArrayMap<String>(16)
        val text = "hello world".toCharArray()
        map["hello"] = "value1"
        map["world"] = "value2"

        map.containsKey(text, 0, 5).shouldBeTrue()
        map.containsKey(text, 6, 5).shouldBeTrue()
        map.containsKey(text, 0, 3).shouldBeFalse()
    }

    @Test
    fun `remove entry`() {
        val map = CharArrayMap<String>(16)
        map["test"] = "value"
        map shouldHaveSize 1

        map.remove("test")

        // Remove가 호출되었으므로 키가 더 이상 존재하지 않아야 함
        map.containsKey("test").shouldBeFalse()
    }

    @Test
    fun `remove non-existent key`() {
        val map = CharArrayMap<String>(16)

        map.remove("nonexistent")

        map shouldHaveSize 0
    }

    @Test
    fun `clear map`() {
        val map = CharArrayMap<String>(16)
        map["key1"] = "value1"
        map["key2"] = "value2"

        map.clear()

        map shouldHaveSize 0
        map.isEmpty().shouldBeTrue()
        map.containsKey("key1").shouldBeFalse()
    }

    @Test
    fun `get non-existent key`() {
        val map = CharArrayMap<String>(16)

        map["nonexistent"].shouldBeNull()
    }

    @Test
    fun `rehash on many insertions`() {
        val map = CharArrayMap<String>(8)

        // Insert many elements to trigger rehash
        repeat(100) { i ->
            map["key$i"] = "value$i"
        }

        map shouldHaveSize 100

        // Verify all entries are still accessible
        repeat(100) { i ->
            map["key$i"] shouldBeEqualTo "value$i"
        }
    }

    @Test
    fun `copy constructor`() {
        val original = CharArrayMap<String>(16)
        original["key1"] = "value1"
        original["key2"] = "value2"

        val copy = CharArrayMap(original)

        copy shouldHaveSize original.size
        copy["key1"] shouldBeEqualTo "value1"
        copy["key2"] shouldBeEqualTo "value2"
    }

    @Test
    fun `copy from regular Map`() {
        val regularMap =
            mutableMapOf<Any, String>(
                "key1" as Any to "value1",
                "key2" as Any to "value2",
            )

        val charArrayMap = CharArrayMap(regularMap)

        charArrayMap shouldHaveSize 2
        charArrayMap["key1"] shouldBeEqualTo "value1"
        charArrayMap["key2"] shouldBeEqualTo "value2"
    }

    @Test
    fun `empty map singleton`() {
        val empty1 = CharArrayMap.emptyMap<String>()

        empty1.isEmpty().shouldBeTrue()
        empty1 shouldHaveSize 0
    }

    @Test
    fun `keys returns key set`() {
        val map = CharArrayMap<String>(16)
        map["key1"] = "value1"
        map["key2"] = "value2"

        val keys = map.keys

        keys shouldHaveSize 2
    }

    @Test
    fun `originalKeySet returns all keys`() {
        val map = CharArrayMap<String>(16)
        map["key1"] = "value1"
        map["key2"] = "value2"

        val keys = map.originalKeySet

        keys shouldHaveSize 2
    }

    @Test
    fun `values collection`() {
        val map = CharArrayMap<String>(16)
        map["key1"] = "value1"
        map["key2"] = "value2"

        val values = map.values

        values shouldHaveSize 2
    }

    @Test
    fun `entries`() {
        val map = CharArrayMap<String>(16)
        map["key1"] = "value1"

        val entries = map.entries

        entries shouldHaveSize 1
    }

    @Test
    fun `setValue updates map entry`() {
        val map = CharArrayMap<String>(16)
        map["key1"] = "original"

        val entry = map.entries.first()
        entry.setValue("updated")

        map["key1"] shouldBeEqualTo "updated"
    }

    @Test
    fun `putAll adds all entries`() {
        val map = CharArrayMap<String>(16)
        val other =
            mutableMapOf<Any, String>(
                "key1" as Any to "value1",
                "key2" as Any to "value2",
            )

        map.putAll(other)

        map shouldHaveSize 2
        map["key1"] shouldBeEqualTo "value1"
        map["key2"] shouldBeEqualTo "value2"
    }

    @Test
    fun `collision handling - different keys`() {
        val map = CharArrayMap<String>(16)

        // Insert elements that might have hash collisions
        map["Aa"] = "value1"
        map["BB"] = "value2"

        map["Aa"] shouldBeEqualTo "value1"
        map["BB"] shouldBeEqualTo "value2"
    }


    @Test
    fun unmodifiableMapBlocksMutationOperations() {
        val source = CharArrayMap<String>(4)
        source["alpha"] = "one"
        val readonly = CharArrayMap.unmodifiableMap(source)

        readonly["alpha"] shouldBeEqualTo "one"
        readonly.entries shouldHaveSize 1

        assertFailsWith<UnsupportedOperationException> { readonly["beta"] = "two" }
        assertFailsWith<UnsupportedOperationException> { readonly.put("gamma" as CharSequence, "three") }
        assertFailsWith<UnsupportedOperationException> { readonly.put("delta".toCharArray(), "four") }
        assertFailsWith<UnsupportedOperationException> { readonly.remove("alpha") }
        assertFailsWith<UnsupportedOperationException> { readonly.remove("alpha", "one") }
        assertFailsWith<UnsupportedOperationException> { readonly.clear() }
        assertFailsWith<UnsupportedOperationException> { readonly.entries.first().setValue("updated") }
        assertFailsWith<UnsupportedOperationException> { readonly.entries.clear() }
    }

    @Test
    fun unmodifiableMapReturnsSharedEmptyMapForEmptySource() {
        val readonly = CharArrayMap.unmodifiableMap(CharArrayMap<String>(0))
        val chars = "missing".toCharArray()

        readonly.isEmpty().shouldBeTrue()
        readonly.containsKey("missing").shouldBeFalse()
        readonly.containsKey(chars).shouldBeFalse()
        readonly.containsKey(chars, 0, chars.size).shouldBeFalse()
        readonly["missing"].shouldBeNull()
        readonly.get(chars, 0, chars.size).shouldBeNull()
        assertFailsWith<UnsupportedOperationException> { readonly["x"] = "y" }
    }

    @Test
    fun companionCopyCreatesIndependentCharArrayMap() {
        val source = mutableMapOf<Any, String>("alpha" as Any to "one", "beta" as Any to "two")
        val copied = CharArrayMap.copy(source)

        copied shouldHaveSize 2
        copied["alpha"] shouldBeEqualTo "one"
        source["alpha"] = "changed"
        copied["alpha"] shouldBeEqualTo "one"
    }

    @Test
    fun entryIteratorExposesKeyValueAndSupportsSetValue() {
        val map = CharArrayMap<String>(4)
        map["alpha"] = "one"
        map["beta"] = "two"

        val iterator = map.EntryIterator(true)
        val firstKey = iterator.nextKeyString()
        val oldValue = iterator.currentValue().shouldNotBeNull()

        iterator.setValue("updated") shouldBeEqualTo oldValue
        map[firstKey] shouldBeEqualTo "updated"
        assertFailsWith<UnsupportedOperationException> { iterator.remove() }
    }

    @Test
    fun entrySetContainsEntriesAndRejectsDirectMutation() {
        val map = CharArrayMap<String>(4)
        map["alpha"] = "one"
        val entry = map.entries.first()

        map.entries.contains(entry).shouldBeTrue()
        (entry.key is CharArray).shouldBeTrue()
        entry.value shouldBeEqualTo "one"
        entry.toString() shouldBeEqualTo "alpha=one"
        assertFailsWith<UnsupportedOperationException> { map.entries.add(entry) }
        assertFailsWith<UnsupportedOperationException> { map.entries.remove(entry) }
    }

    @Test
    fun keySetsRejectAddAndOriginalIteratorRejectsRemove() {
        val map = CharArrayMap<String>(4)
        map["alpha"] = "one"

        assertFailsWith<UnsupportedOperationException> { map.keys.add("beta") }
        assertFailsWith<UnsupportedOperationException> { map.keys.add("beta" as CharSequence) }
        assertFailsWith<UnsupportedOperationException> { map.keys.add("beta".toCharArray()) }
        assertFailsWith<UnsupportedOperationException> { map.originalKeySet.add("beta") }

        val iterator = map.originalKeySet.iterator()
        iterator.hasNext().shouldBeTrue()
        String(iterator.next() as CharArray) shouldBeEqualTo "alpha"
        assertFailsWith<UnsupportedOperationException> { iterator.remove() }
    }

    @Test
    fun mapToStringUsesEntryRendering() {
        val map = CharArrayMap<Any>(4)
        map["alpha"] = "one"
        map["self"] = map

        val rendered = map.toString()
        rendered.contains("alpha=one").shouldBeTrue()
        rendered.contains("self=(this Map)").shouldBeTrue()
    }

}
