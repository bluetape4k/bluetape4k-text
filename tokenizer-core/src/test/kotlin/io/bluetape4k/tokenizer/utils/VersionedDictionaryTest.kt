package io.bluetape4k.tokenizer.utils

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import org.junit.jupiter.api.Test

/** 버전 증가, 원자적 실패 및 rollback 계약을 검증합니다. */
class VersionedDictionaryTest {

    @Test
    fun `높은 revision으로 reload하고 이전 snapshot으로 rollback한다`() {
        val store = VersionedDictionary(DictionarySnapshot(DictionaryVersion("test", 1), setOf("old")))

        store.reload(DictionaryVersion("test", 2)) { setOf("new") }
        store.snapshot().value shouldBeEqualTo setOf("new")

        store.rollback().value shouldBeEqualTo setOf("old")
    }

    @Test
    fun `loader 실패는 현재 snapshot을 변경하지 않는다`() {
        val initial = DictionarySnapshot(DictionaryVersion("test", 1), setOf("old"))
        val store = VersionedDictionary(initial)

        assertFailsWith<IllegalStateException> {
            store.reload(DictionaryVersion("test", 2)) {
                error("broken source")
            }
        }

        store.snapshot() shouldBeEqualTo initial
    }

    @Test
    fun `revision과 이름 규칙을 검증한다`() {
        assertFailsWith<IllegalArgumentException> { DictionaryVersion("", 0) }
        assertFailsWith<IllegalArgumentException> { DictionaryVersion("test", -1) }

        val store = VersionedDictionary(DictionarySnapshot(DictionaryVersion("test", 1), "value"))
        assertFailsWith<IllegalArgumentException> {
            store.reload(DictionaryVersion("other", 2)) { "other" }
        }
        assertFailsWith<IllegalArgumentException> {
            store.reload(DictionaryVersion("test", 1)) { "same" }
        }
    }
}
