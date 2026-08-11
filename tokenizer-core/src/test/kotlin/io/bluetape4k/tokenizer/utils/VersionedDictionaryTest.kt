package io.bluetape4k.tokenizer.utils

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import org.junit.jupiter.api.Test
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

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

    @Test
    fun `history capacity는 음수가 될 수 없다`() {
        assertFailsWith<IllegalArgumentException> {
            VersionedDictionary(
                DictionarySnapshot(DictionaryVersion("test", 1), "value"),
                historyCapacity = -1,
            )
        }
    }

    @Test
    fun `history capacity만큼만 rollback snapshot을 보존한다`() {
        val store = VersionedDictionary(
            DictionarySnapshot(DictionaryVersion("test", 1), "v1"),
            historyCapacity = 2,
        )

        store.reload(DictionaryVersion("test", 2)) { "v2" }
        store.reload(DictionaryVersion("test", 3)) { "v3" }
        store.reload(DictionaryVersion("test", 4)) { "v4" }

        store.rollback().value shouldBeEqualTo "v3"
        store.rollback().value shouldBeEqualTo "v2"
        assertFailsWith<IllegalStateException> { store.rollback() }
    }

    @Test
    fun `zero history capacity는 rollback journal을 비활성화한다`() {
        val store = VersionedDictionary(
            DictionarySnapshot(DictionaryVersion("test", 1), "v1"),
            historyCapacity = 0,
        )

        store.reload(DictionaryVersion("test", 2)) { "v2" }

        assertFailsWith<IllegalStateException> { store.rollback() }
        store.snapshot().value shouldBeEqualTo "v2"
    }

    @Test
    fun `기본 history capacity는 one-step rollback만 제공한다`() {
        val store = VersionedDictionary(
            DictionarySnapshot(DictionaryVersion("test", 1), "v1"),
        )

        store.reload(DictionaryVersion("test", 2)) { "v2" }
        store.reload(DictionaryVersion("test", 3)) { "v3" }

        store.rollback().value shouldBeEqualTo "v2"
        assertFailsWith<IllegalStateException> { store.rollback() }
    }

    @Test
    fun `실패한 reload는 기존 journal 순서를 보존한다`() {
        val store = VersionedDictionary(
            DictionarySnapshot(DictionaryVersion("test", 1), "v1"),
            historyCapacity = 2,
        )
        store.reload(DictionaryVersion("test", 2)) { "v2" }

        assertFailsWith<IllegalStateException> {
            store.reload(DictionaryVersion("test", 3)) { error("broken source") }
        }
        assertFailsWith<IllegalArgumentException> {
            store.reload(DictionaryVersion("other", 3)) { "wrong name" }
        }
        assertFailsWith<IllegalArgumentException> {
            store.reload(DictionaryVersion("test", 2)) { "wrong revision" }
        }

        store.rollback().value shouldBeEqualTo "v1"
        assertFailsWith<IllegalStateException> { store.rollback() }
    }

    @Test
    fun `동시 snapshot 조회 중에도 writer는 완전한 snapshot을 공개한다`() {
        val store = VersionedDictionary(
            DictionarySnapshot(DictionaryVersion("test", 1), 1),
            historyCapacity = 1,
        )
        val executor = Executors.newFixedThreadPool(4)

        try {
            executor.submit {
                repeat(100) {
                    val current = store.snapshot()
                    store.reload(DictionaryVersion("test", current.version.revision + 1)) {
                        current.value + 1
                    }
                }
            }
            repeat(3) {
                executor.submit {
                    repeat(100) {
                        check(store.snapshot().value >= 1)
                    }
                }
            }
        } finally {
            executor.shutdown()
            executor.awaitTermination(10, TimeUnit.SECONDS)
        }

        store.snapshot().version.revision shouldBeEqualTo 101
    }
}
