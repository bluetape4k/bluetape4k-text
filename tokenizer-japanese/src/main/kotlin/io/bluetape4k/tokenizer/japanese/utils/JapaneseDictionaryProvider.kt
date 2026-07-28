package io.bluetape4k.tokenizer.japanese.utils

import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.tokenizer.utils.CharArraySet
import io.bluetape4k.tokenizer.utils.DictionaryProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

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
        blockWordDictionary.addAll(words)
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
        blockWordDictionary.removeAll(words)
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
        blockWordDictionary.clear()
    }
}
