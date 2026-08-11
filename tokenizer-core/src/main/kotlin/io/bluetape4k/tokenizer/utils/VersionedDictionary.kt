package io.bluetape4k.tokenizer.utils

import java.io.Serializable
import java.util.ArrayDeque
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * 런타임 사전 snapshot을 식별하는 이름과 단조 증가 revision입니다.
 *
 * @property name 사전 종류를 식별하는 비어 있지 않은 이름입니다.
 * @property revision 같은 이름 안에서 증가하는 음이 아닌 버전 번호입니다.
 */
data class DictionaryVersion(
    val name: String,
    val revision: Long,
): Serializable {
    init {
        require(name.isNotBlank()) { "Dictionary version name must not be blank" }
        require(revision >= 0) { "Dictionary version revision must not be negative: $revision" }
    }

    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

/**
 * 사전 값과 해당 값이 생성된 버전을 함께 보관하는 불변 wrapper입니다.
 *
 * @param T 사전 값의 타입입니다. 호출자는 값 자체도 불변으로 유지해야 합니다.
 * @property version snapshot을 만든 사전 버전입니다.
 * @property value 해당 버전에서 원자적으로 공개된 값입니다.
 */
data class DictionarySnapshot<out T>(
    val version: DictionaryVersion,
    val value: T,
)

private const val DEFAULT_HISTORY_CAPACITY: Int = 1

/**
 * 실패한 로드가 현재 값을 훼손하지 않도록 원자적으로 사전을 교체하는 저장소입니다.
 *
 * `reload`는 loader를 lock 밖에서 실행하지 않고 lock 안에서 평가하므로 같은 저장소에 대한
 * 여러 갱신이 순서대로 검증된다. loader가 예외를 던지거나 revision이 현재보다 높지 않으면
 * 현재 snapshot은 그대로 유지된다. 성공한 이전 snapshot은 제한된 journal 범위에서
 * [rollback]으로 되돌릴 수 있다.
 * 값 객체의 내부 mutable state까지 복제하지는 않으므로 loader는 독립적이고 읽기 전용인 값을
 * 반환해야 한다.
 *
 * @param T 사전 값의 타입입니다.
 * @property initial 처음 공개할 사전 snapshot입니다.
 * @property historyCapacity 현재 snapshot을 제외하고 보존할 이전 snapshot 수입니다.
 * 음이 아닌 값이어야 하며 기본값 1은 one-step rollback을 보존합니다. 0이면 journal을
 * 비활성화하고, 기본값에서는 기존 무제한 multi-step rollback을 제공하지 않습니다.
 */
class VersionedDictionary<T> @JvmOverloads constructor(
    initial: DictionarySnapshot<T>,
    private val historyCapacity: Int = DEFAULT_HISTORY_CAPACITY,
) {
    init {
        require(historyCapacity >= 0) {
            "Dictionary history capacity must not be negative: $historyCapacity"
        }
    }

    private val current = AtomicReference(initial)
    private val mutationLock = ReentrantLock()
    private val history = ArrayDeque<DictionarySnapshot<T>>()

    /** 현재 원자적으로 공개된 snapshot을 반환합니다. */
    fun snapshot(): DictionarySnapshot<T> = current.get()

    /**
     * 더 높은 [version]으로 사전을 다시 로드하고 성공한 snapshot을 반환합니다.
     *
     * @param version 새로 공개할 버전입니다. 현재 revision보다 커야 합니다.
     * @param loader 새 사전 값을 만드는 함수입니다. 실패하면 현재 값은 유지됩니다.
     * @return 새로 공개된 snapshot입니다.
     */
    fun reload(version: DictionaryVersion, loader: () -> T): DictionarySnapshot<T> = mutationLock.withLock {
        val previous = current.get()
        require(version.name == previous.version.name) {
            "Dictionary version name mismatch: ${previous.version.name} != ${version.name}"
        }
        require(version.revision > previous.version.revision) {
            "Dictionary revision must increase: ${previous.version.revision} -> ${version.revision}"
        }

        val next = DictionarySnapshot(version, loader())
        if (historyCapacity > 0) {
            history.addLast(previous)
            while (history.size > historyCapacity) {
                history.removeFirst()
            }
        }
        current.set(next)
        next
    }

    /**
     * 지정한 snapshot을 검증 후 공개합니다.
     *
     * @param snapshot 공개할 새 snapshot입니다.
     * @return 공개된 snapshot입니다.
     */
    fun reload(snapshot: DictionarySnapshot<T>): DictionarySnapshot<T> =
        reload(snapshot.version) { snapshot.value }

    /**
     * 가장 최근의 이전 snapshot으로 되돌립니다.
     *
     * @return rollback 후 현재 snapshot입니다.
     * @throws IllegalStateException 되돌릴 이전 snapshot이 없을 때 발생합니다.
     */
    fun rollback(): DictionarySnapshot<T> = mutationLock.withLock {
        check(history.isNotEmpty()) { "No previous dictionary snapshot is available" }
        val previous = history.removeLast()
        current.set(previous)
        previous
    }
}
