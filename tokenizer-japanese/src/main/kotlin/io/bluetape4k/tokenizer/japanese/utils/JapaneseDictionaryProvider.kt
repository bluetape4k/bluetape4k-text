package io.bluetape4k.tokenizer.japanese.utils

import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.tokenizer.model.Severity
import io.bluetape4k.tokenizer.utils.CharArraySet
import io.bluetape4k.tokenizer.utils.DictionarySnapshot
import io.bluetape4k.tokenizer.utils.DictionaryProvider
import io.bluetape4k.tokenizer.utils.DictionaryVersion
import io.bluetape4k.tokenizer.utils.VersionedDictionary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.util.Collections
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

private class JapaneseBlockwordValue(
    val wordsBySeverity: Map<Severity, Set<String>>,
) {
    val words: Set<String> = wordsBySeverity.values.asSequence().flatten().toSet()
}

/**
 * 일본어 토크나이저가 사용하는 금칙어 사전을 로드하고 관리합니다.
 *
 * 사전 파일은 [BASE_PATH](`japanesetext`) 기준 상대 경로로 해석하고
 * [io.bluetape4k.tokenizer.utils.DictionaryProvider]로 로드합니다.
 * [blockWordDictionary]는 현재 severity cumulative snapshot의 read-only 호환 view를 반환합니다.
 *
 * ```kotlin
 * val hasWord = JapaneseDictionaryProvider.blockWordDictionary.contains("性器")
 *
 * // hasWord == true
 * ```
 */
@Suppress("TooManyFunctions")
object JapaneseDictionaryProvider: KLoggingChannel() {

    private val dictionaryMutationLock = ReentrantLock()

    private val blockwordVersions by lazy {
        val wordsBySeverity = runBlocking(Dispatchers.IO) {
            readWordsBySeverity()
        }
        VersionedDictionary(
            DictionarySnapshot(
                DictionaryVersion("japanese-blockwords", 0),
                snapshotBlockwordValue(wordsBySeverity),
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
     *     JapaneseDictionaryProvider.readWordsAsSet("noun/nouns.txt")
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
     *     JapaneseDictionaryProvider.readWords("noun/nouns.txt")
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

    private suspend fun readWordsBySeverity(): Map<Severity, Set<String>> = coroutineScope {
        val allWords = async {
            readWords("block/blocks.txt")
                .map { it.asDictionaryWord() }
                .toSet()
        }
        val overrides = async {
            withContext(Dispatchers.IO) {
                DictionaryProvider.readFileByLineFromResources("$BASE_PATH/block/blocks_severity.tsv")
                    .filter(String::isNotBlank)
                    .map { line ->
                        val fields = line.split('\t', limit = 2)
                        require(fields.size == 2) { "Invalid Japanese blockword entry" }
                        Severity.valueOf(fields[0]) to fields[1]
                    }
                    .groupBy({ it.first }, { it.second })
                    .mapValues { (_, words) -> words.toSet() }
            }
        }
        val base = allWords.await()
        val tagged = overrides.await()
        val overridden = tagged.values.asSequence().flatten().toSet()
        mapOf(
            Severity.LOW to base - overridden,
            Severity.MIDDLE to tagged[Severity.MIDDLE].orEmpty(),
            Severity.HIGH to tagged[Severity.HIGH].orEmpty(),
        )
    }

    /**
     * 최초 접근 시 `block/blocks.txt`에서 lazy 로드하는 인메모리 금칙어 사전입니다.
     * `blocks.txt`의 기존 전체 목록과 `blocks_severity.tsv`의 exact-tier override를
     * 함께 읽으며, severity 조회는 한국어 processor와 같은 cumulative threshold 정책을 사용합니다.
     *
     * [addBlockwords], [removeBlockwords], [clearBlockwords]로 수행한 변경은 다음 조회부터
     * 새 immutable snapshot에 반영됩니다. 반환값은 read-only 호환 view이며 직접 쓰기를
     * 시도하면 `UnsupportedOperationException`이 발생합니다.
     *
     * ```kotlin
     * val dictionary = JapaneseDictionaryProvider.blockWordDictionary
     *
     * // dictionary.contains("性器") == true
     * ```
     */
    val blockWordDictionary: CharArraySet
        get() = CharArraySet.unmodifiableSet(
            CharArraySet(blockwordVersions.snapshot().value.words.toList())
        )

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
        addBlockwords(words, Severity.DEFAULT)
    }

    /**
     * 지정한 severity tier에 런타임 금칙어를 추가합니다.
     *
     * @param words 추가할 금칙어 단어 컬렉션입니다.
     * @param severity 추가할 exact severity tier입니다.
     */
    fun addBlockwords(words: Collection<String>, severity: Severity) {
        log.debug { "금칙어를 추가합니다. count=${words.size}, totalLength=${words.sumOf { it.length }}" }
        dictionaryMutationLock.withLock {
            val current = blockwordVersions.snapshot()
            val exactTiers = exactBlockwordValue(current.value.wordsBySeverity).toMutableMap()
            val currentWords = exactTiers.getValue(severity)
            val nextWords = immutableSet(currentWords + words)
            val next = if (nextWords == currentWords) {
                current.value
            } else {
                exactTiers[severity] = nextWords
                snapshotBlockwordValue(exactTiers)
            }
            publishBlockwordMutation(next)
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
            val current = blockwordVersions.snapshot()
            val exactTiers = exactBlockwordValue(current.value.wordsBySeverity).toMutableMap()
            val wordsToRemove = words.toSet()
            val nextTiers = exactTiers.mapValues { (_, tierWords) -> tierWords - wordsToRemove }
            val next = if (nextTiers == exactTiers) {
                current.value
            } else {
                snapshotBlockwordValue(nextTiers)
            }
            publishBlockwordMutation(next)
        }
    }

    /**
     * 지정한 severity tier에서만 런타임 금칙어를 제거합니다.
     *
     * @param words 제거할 금칙어 단어 컬렉션입니다.
     * @param severity 제거할 exact severity tier입니다.
     */
    fun removeBlockwords(words: Collection<String>, severity: Severity) {
        log.debug { "금칙어를 제거합니다. count=${words.size}, totalLength=${words.sumOf { it.length }}" }
        dictionaryMutationLock.withLock {
            val current = blockwordVersions.snapshot()
            val exactTiers = exactBlockwordValue(current.value.wordsBySeverity).toMutableMap()
            val currentWords = exactTiers.getValue(severity)
            val nextWords = immutableSet(currentWords - words.toSet())
            val next = if (nextWords == currentWords) {
                current.value
            } else {
                exactTiers[severity] = nextWords
                snapshotBlockwordValue(exactTiers)
            }
            publishBlockwordMutation(next)
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
            val current = blockwordVersions.snapshot()
            publishBlockwordMutation(
                if (current.value.words.isEmpty()) current.value else snapshotBlockwordValue(emptyMap())
            )
        }
    }

    /** 현재 일본어 금칙어 snapshot과 버전을 반환합니다. */
    fun currentBlockwordSnapshot(): DictionarySnapshot<Set<String>> {
        val current = blockwordVersions.snapshot()
        return DictionarySnapshot(current.version, current.value.words)
    }

    /**
     * 현재 일본어 금칙어의 severity cumulative snapshot과 버전을 반환합니다.
     *
     * `LOW`는 모든 tier, `MIDDLE`은 middle/high, `HIGH`는 high tier만 포함합니다.
     */
    fun currentBlockwordSeveritySnapshot(): DictionarySnapshot<Map<Severity, Set<String>>> {
        val current = blockwordVersions.snapshot()
        return DictionarySnapshot(current.version, current.value.wordsBySeverity)
    }

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
        val updated = reloadBlockwordValue(version, mapOf(Severity.DEFAULT to words))
        DictionarySnapshot(updated.version, updated.value.words)
    }

    /**
     * severity별 exact tier를 받아 일본어 금칙어 사전을 새 버전으로 교체합니다.
     *
     * @param version 현재 버전보다 큰 `japanese-blockwords` 버전입니다.
     * @param wordsBySeverity exact severity별 전체 금칙어 목록입니다.
     * @return 공개된 cumulative severity snapshot입니다.
     */
    fun reloadBlockwords(
        version: DictionaryVersion,
        wordsBySeverity: Map<Severity, Collection<String>>,
    ): DictionarySnapshot<Map<Severity, Set<String>>> = dictionaryMutationLock.withLock {
        val updated = reloadBlockwordValue(version, wordsBySeverity)
        DictionarySnapshot(updated.version, updated.value.wordsBySeverity)
    }

    /** 지정한 단어가 현재 일본어 금칙어 사전에 있는지 확인합니다. */
    fun containsBlockword(text: String): Boolean =
        blockwordVersions.snapshot().value.words.contains(text)

    /**
     * 지정한 severity threshold에서 단어가 금칙어인지 확인합니다.
     *
     * @param text 확인할 단어입니다.
     * @param severity 적용할 cumulative severity threshold입니다.
     */
    fun containsBlockword(text: String, severity: Severity): Boolean =
        blockwordVersions.snapshot().value.wordsBySeverity[severity].orEmpty().contains(text)

    private fun snapshotBlockwordValue(wordsBySeverity: Map<Severity, Collection<String>>): JapaneseBlockwordValue =
        JapaneseBlockwordValue(canonicalBlockwordValue(wordsBySeverity))

    private fun reloadBlockwordValue(
        version: DictionaryVersion,
        wordsBySeverity: Map<Severity, Collection<String>>,
    ): DictionarySnapshot<JapaneseBlockwordValue> {
        require(version.name == "japanese-blockwords") { "Expected japanese-blockwords version" }
        val replacement = snapshotBlockwordValue(wordsBySeverity)
        return blockwordVersions.reload(version) { replacement }
    }

    private fun canonicalBlockwordValue(
        wordsBySeverity: Map<Severity, Collection<String>>,
    ): Map<Severity, Set<String>> {
        val exactTiers = Severity.values().associateWith { severity ->
            immutableSet(wordsBySeverity[severity].orEmpty())
        }
        return immutableMap(
            mapOf(
                Severity.LOW to exactTiers.getValue(Severity.LOW) +
                        exactTiers.getValue(Severity.MIDDLE) + exactTiers.getValue(Severity.HIGH),
                Severity.MIDDLE to exactTiers.getValue(Severity.MIDDLE) + exactTiers.getValue(Severity.HIGH),
                Severity.HIGH to exactTiers.getValue(Severity.HIGH),
            )
        )
    }

    private fun exactBlockwordValue(
        cumulativeValue: Map<Severity, Set<String>>,
    ): Map<Severity, Set<String>> {
        val high = cumulativeValue[Severity.HIGH].orEmpty().toSet()
        val middle = cumulativeValue[Severity.MIDDLE].orEmpty().toSet() - high
        val low = cumulativeValue[Severity.LOW].orEmpty().toSet() - middle - high
        return mapOf(
            Severity.LOW to immutableSet(low),
            Severity.MIDDLE to immutableSet(middle),
            Severity.HIGH to immutableSet(high),
        )
    }

    private fun Any.asDictionaryWord(): String = when (this) {
        is CharArray -> concatToString()
        else -> toString()
    }

    private fun publishBlockwordMutation(value: JapaneseBlockwordValue) {
        val current = blockwordVersions.snapshot()
        blockwordVersions.reload(
            DictionaryVersion(current.version.name, current.version.revision + 1)
        ) { value }
    }

}

private fun <K, V> immutableMap(value: Map<K, V>): Map<K, V> = Collections.unmodifiableMap(value.toMap())

private fun immutableSet(value: Collection<String>): Set<String> = Collections.unmodifiableSet(value.toSet())
