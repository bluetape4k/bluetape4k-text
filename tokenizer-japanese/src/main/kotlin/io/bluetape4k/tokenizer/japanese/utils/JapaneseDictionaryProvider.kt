package io.bluetape4k.tokenizer.japanese.utils

import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.tokenizer.utils.CharArraySet
import io.bluetape4k.tokenizer.utils.DictionarySnapshot
import io.bluetape4k.tokenizer.utils.DictionaryProvider
import io.bluetape4k.tokenizer.utils.DictionaryVersion
import io.bluetape4k.tokenizer.utils.VersionedDictionary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * 일본어 토크나이저가 사용하는 금칙어 사전을 로드하고 관리합니다.
 *
 * 사전 파일은 [BASE_PATH](`japanesetext`) 기준 상대 경로로 해석하고
 * [io.bluetape4k.tokenizer.utils.DictionaryProvider]로 로드합니다.
 * [blockWordDictionary]는 최초 접근 시 lazy 초기화한 뒤 객체 수명 동안 재사용합니다.
 *
 * ```kotlin
 * val hasWord = JapaneseDictionaryProvider.blockWordDictionary.contains("性器")
 *
 * // hasWord == true
 * ```
 */
object JapaneseDictionaryProvider: KLoggingChannel() {

    private val dictionaryMutationLock = ReentrantLock()

    private val blockwordVersions by lazy {
        VersionedDictionary(
            DictionarySnapshot(
                DictionaryVersion("japanese-blockwords", 0),
                snapshotBlockwordValue(),
            ),
            historyCapacity = 0,
        )
    }

    /** 모든 일본어 사전 리소스의 classpath 루트 접두사입니다(`japanesetext`). */
    const val BASE_PATH = "japanesetext"

    /**
     * 지정한 사전 파일을 읽어 내용을 [MutableSet]으로 반환합니다.
     *
     * 경로는 [BASE_PATH] 기준 상대 경로로 해석합니다. 중복 단어는 set 특성으로 제거됩니다.
     *
     * ```kotlin
     * val words = kotlinx.coroutines.runBlocking {
     *     JapaneseDictionaryProvider.readWordsAsSet("block/blocks.txt")
     * }
     *
     * // result == true (words.isNotEmpty())
     * ```
     *
     * @param paths [BASE_PATH] 기준의 상대 사전 파일 경로 목록입니다.
     * @return 중복을 제거한 mutable 문자열 집합입니다.
     */
    suspend fun readWordsAsSet(vararg paths: String): MutableSet<String> {
        return DictionaryProvider.readWordsAsSet(*paths.map { "$BASE_PATH/$it" }.toTypedArray())
    }

    /**
     * 지정한 사전 파일을 읽어 내용을 [CharArraySet]으로 반환합니다.
     *
     * 경로는 [BASE_PATH] 기준 상대 경로로 해석합니다.
     *
     * ```kotlin
     * val words = kotlinx.coroutines.runBlocking {
     *     JapaneseDictionaryProvider.readWords("block/blocks.txt")
     * }
     *
     * // result == true (words.isNotEmpty())
     * ```
     *
     * @param paths [BASE_PATH] 기준의 상대 사전 파일 경로 목록입니다.
     * @return 금칙어 조회에 쓰는 [CharArraySet]입니다.
     */
    suspend fun readWords(vararg paths: String): CharArraySet {
        return DictionaryProvider.readWords(*paths.map { "$BASE_PATH/$it" }.toTypedArray())
    }

    /**
     * 최초 접근 시 `block/blocks.txt`에서 lazy 로드하는 인메모리 금칙어 사전입니다.
     *
     * [addBlockwords], [removeBlockwords], [clearBlockwords]로 수행한 변경은 즉시 반영됩니다.
     * 반환된 mutable collection에 직접 쓰는 변경은 versioned snapshot에 기록되지 않습니다.
     *
     * ```kotlin
     * val dictionary = JapaneseDictionaryProvider.blockWordDictionary
     *
     * // dictionary.contains("性器") == true
     * ```
     */
    val blockWordDictionary: CharArraySet by lazy {
        runBlocking(Dispatchers.IO) {
            readWords("block/blocks.txt")
        }
    }

    /**
     * 인메모리 금칙어 사전에 단어를 추가합니다. 중복 단어는 무시됩니다.
     *
     * ```kotlin
     * JapaneseDictionaryProvider.addBlockwords(listOf("19禁", "29禁"))
     * val value = JapaneseDictionaryProvider.blockWordDictionary.contains("19禁")
     *
     * // value == true
     * ```
     *
     * @param words 추가할 금칙어 단어 컬렉션입니다.
     */
    fun addBlockwords(words: Collection<String>) {
        log.debug { "금칙어를 추가합니다. count=${words.size}, totalLength=${words.sumOf { it.length }}" }
        dictionaryMutationLock.withLock {
            blockWordDictionary.addAll(words)
            publishBlockwordMutation()
        }
    }

    /**
     * 인메모리 금칙어 사전에서 단어를 제거합니다. 등록되지 않은 단어는 무시합니다.
     *
     * ```kotlin
     * JapaneseDictionaryProvider.removeBlockwords(listOf("19禁"))
     * val value = JapaneseDictionaryProvider.blockWordDictionary.contains("19禁")
     *
     * // value == false
     * ```
     *
     * @param words 제거할 금칙어 단어 컬렉션입니다.
     */
    fun removeBlockwords(words: Collection<String>) {
        log.debug { "금칙어를 제거합니다. count=${words.size}, totalLength=${words.sumOf { it.length }}" }
        dictionaryMutationLock.withLock {
            blockWordDictionary.removeAll(words)
            publishBlockwordMutation()
        }
    }

    /**
     * 인메모리 금칙어 사전을 비웁니다. 원본 리소스 파일은 수정하지 않습니다.
     *
     * ```kotlin
     * JapaneseDictionaryProvider.clearBlockwords()
     * val value = JapaneseDictionaryProvider.blockWordDictionary.isEmpty()
     *
     * // value == true
     * ```
     */
    fun clearBlockwords() {
        log.debug { "금칙어 사전을 비웁니다" }
        dictionaryMutationLock.withLock {
            blockWordDictionary.clear()
            publishBlockwordMutation()
        }
    }

    /** 현재 일본어 금칙어 snapshot과 버전을 반환합니다. */
    fun currentBlockwordSnapshot(): DictionarySnapshot<Set<String>> = blockwordVersions.snapshot()

    /**
     * 일본어 금칙어 사전을 새 버전으로 교체합니다.
     *
     * @param version 현재 버전보다 큰 `japanese-blockwords` 버전입니다.
     * @param words 새 전체 금칙어 목록입니다.
     * @return 공개된 금칙어 snapshot입니다.
     */
    fun reloadBlockwords(
        version: DictionaryVersion,
        words: Collection<String>,
    ): DictionarySnapshot<Set<String>> = dictionaryMutationLock.withLock {
        require(version.name == "japanese-blockwords") { "Expected japanese-blockwords version" }
        val replacement = words.toSet()
        val snapshot = blockwordVersions.reload(version) { replacement }
        blockWordDictionary.clear()
        blockWordDictionary.addAll(replacement)
        snapshot
    }

    /** 지정한 단어가 현재 일본어 금칙어 사전에 있는지 확인합니다. */
    fun containsBlockword(text: String): Boolean =
        dictionaryMutationLock.withLock { blockWordDictionary.contains(text) }

    private fun snapshotBlockwordValue(): Set<String> =
        blockWordDictionary.map { it.asDictionaryWord() }.toSet()

    private fun Any.asDictionaryWord(): String = when (this) {
        is CharArray -> concatToString()
        else -> toString()
    }

    private fun publishBlockwordMutation() {
        val current = blockwordVersions.snapshot()
        blockwordVersions.reload(
            DictionaryVersion(current.version.name, current.version.revision + 1)
        ) { snapshotBlockwordValue() }
    }
}
