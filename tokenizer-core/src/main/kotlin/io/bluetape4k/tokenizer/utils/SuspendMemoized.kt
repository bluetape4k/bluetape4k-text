package io.bluetape4k.tokenizer.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * suspend initializer의 성공 결과를 한 번만 보관하는 memoizer입니다.
 *
 * [get]은 동시 호출을 하나의 초기화로 합치고 성공한 값만 공개합니다. initializer가
 * 예외를 던지거나 호출 코루틴이 취소되면 값을 저장하지 않으므로 다음 호출에서 다시
 * 초기화합니다. [getBlocking]은 기존 동기 facade를 위해 `Dispatchers.IO`에서 같은
 * 초기화를 기다리며, 대기 중 호출 스레드가 interrupt되면 [InterruptedException]을
 * 다시 던지기 전에 interrupt flag를 복구합니다.
 *
 * [clear]는 명시적으로 성공 값을 제거하며, provider의 테스트 lifecycle 격리에 사용합니다.
 * [isInitialized]는 현재 성공 값이 공개되었는지 확인합니다.
 *
 * @param T memoized 결과 타입입니다.
 * @param initializer 성공 값을 만드는 suspend initializer입니다.
 */
class SuspendMemoized<T: Any>(
    private val initializer: suspend () -> T,
) {

    private object Uninitialized

    private val mutex = Mutex()

    @Volatile
    private var state: Any = Uninitialized

    /** 성공한 초기화 결과가 현재 공개되었는지 확인합니다. */
    fun isInitialized(): Boolean = state !== Uninitialized

    @Suppress("UNCHECKED_CAST")
    private fun currentValueOrNull(): T? {
        val current = state
        return if (current === Uninitialized) null else current as T
    }

    /**
     * 성공한 결과를 반환합니다.
     *
     * 동시 호출은 하나의 initializer 실행을 공유합니다. 실패하거나 취소된 initializer의
     * 결과는 저장하지 않으며, 성공 결과만 다음 호출에서 재사용합니다.
     */
    suspend fun get(): T {
        currentValueOrNull()?.let { return it }

        return mutex.withLock {
            currentValueOrNull() ?: initializer().also { state = it }
        }
    }

    /**
     * 성공한 결과를 동기적으로 반환합니다.
     *
     * 아직 초기화되지 않았다면 `Dispatchers.IO`에서 [get]을 기다립니다. 호출 스레드가
     * 대기 중 interrupt되면 [InterruptedException]을 그대로 전달하고 interrupt flag를
     * 복구합니다. 실패하거나 취소된 초기화는 저장하지 않으므로 이후 호출이 재시도합니다.
     */
    fun getBlocking(): T {
        return currentValueOrNull() ?: run {
            try {
                runBlocking(Dispatchers.IO) { get() }
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                throw e
            }
        }
    }

    /** 현재 성공 값을 제거하여 다음 호출이 initializer를 다시 실행하도록 합니다. */
    suspend fun clear() {
        mutex.withLock {
            state = Uninitialized
        }
    }
}
