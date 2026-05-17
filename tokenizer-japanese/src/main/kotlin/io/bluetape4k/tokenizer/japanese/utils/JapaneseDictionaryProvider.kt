package io.bluetape4k.tokenizer.japanese.utils

import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.tokenizer.utils.CharArraySet
import io.bluetape4k.tokenizer.utils.DictionaryProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

/**
 * Loads and manages the blockword dictionary used by the Japanese tokenizer.
 *
 * Dictionary files are resolved relative to [BASE_PATH] (`japanesetext`) and loaded
 * via [io.bluetape4k.tokenizer.utils.DictionaryProvider]. The [blockWordDictionary]
 * is lazily initialized on first access and reused for the object's lifetime.
 *
 * ```kotlin
 * val hasWord = JapaneseDictionaryProvider.blockWordDictionary.contains("性器")
 *
 * // hasWord == true
 * ```
 */
object JapaneseDictionaryProvider: KLoggingChannel() {

    /** Root classpath prefix for all Japanese dictionary resources (`japanesetext`). */
    const val BASE_PATH = "japanesetext"

    /**
     * Reads the specified dictionary files and returns their contents as a [MutableSet].
     *
     * Paths are resolved relative to [BASE_PATH]. Duplicate words are deduplicated by the set.
     *
     * ```kotlin
     * val words = kotlinx.coroutines.runBlocking {
     *     JapaneseDictionaryProvider.readWordsAsSet("block/blocks.txt")
     * }
     *
     * // result == true (words.isNotEmpty())
     * ```
     */
    suspend fun readWordsAsSet(vararg paths: String): MutableSet<String> {
        return DictionaryProvider.readWordsAsSet(*paths.map { "$BASE_PATH/$it" }.toTypedArray())
    }

    /**
     * Reads the specified dictionary files and returns their contents as a [CharArraySet].
     *
     * Paths are resolved relative to [BASE_PATH].
     *
     * ```kotlin
     * val words = kotlinx.coroutines.runBlocking {
     *     JapaneseDictionaryProvider.readWords("block/blocks.txt")
     * }
     *
     * // result == true (words.isNotEmpty())
     * ```
     */
    suspend fun readWords(vararg paths: String): CharArraySet {
        return DictionaryProvider.readWords(*paths.map { "$BASE_PATH/$it" }.toTypedArray())
    }

    /**
     * In-memory blockword dictionary, lazily loaded from `block/blocks.txt` on first access.
     *
     * Mutations via [addBlockwords], [removeBlockwords], and [clearBlockwords] take effect immediately.
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
     * Adds words to the in-memory blockword dictionary. Duplicates are ignored.
     *
     * ```kotlin
     * JapaneseDictionaryProvider.addBlockwords(listOf("19禁", "29禁"))
     * val value = JapaneseDictionaryProvider.blockWordDictionary.contains("19禁")
     *
     * // value == true
     * ```
     */
    fun addBlockwords(words: Collection<String>) {
        log.debug { "Add block words: ${words.joinToString(",")}" }
        blockWordDictionary.addAll(words)
    }

    /**
     * Removes words from the in-memory blockword dictionary. Unknown words are silently ignored.
     *
     * ```kotlin
     * JapaneseDictionaryProvider.removeBlockwords(listOf("19禁"))
     * val value = JapaneseDictionaryProvider.blockWordDictionary.contains("19禁")
     *
     * // value == false
     * ```
     */
    fun removeBlockwords(words: Collection<String>) {
        log.debug { "Remove block words: ${words.joinToString(",")}" }
        blockWordDictionary.removeAll(words)
    }

    /**
     * Clears the in-memory blockword dictionary. Does not modify the underlying resource files.
     *
     * ```kotlin
     * JapaneseDictionaryProvider.clearBlockwords()
     * val value = JapaneseDictionaryProvider.blockWordDictionary.isEmpty()
     *
     * // value == true
     * ```
     */
    fun clearBlockwords() {
        log.debug { "Clear block words" }
        blockWordDictionary.clear()
    }
}
