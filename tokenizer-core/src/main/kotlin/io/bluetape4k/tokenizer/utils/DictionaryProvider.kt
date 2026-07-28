package io.bluetape4k.tokenizer.utils

import io.bluetape4k.coroutines.flow.async
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.InputStreamReader
import java.util.zip.GZIPInputStream

/**
 * Classpath resource dictionary file을 읽어 tokenizer input structure로 변환하는 utility입니다.
 *
 * ## 동작 계약
 * - Line은 UTF-8로 읽고 trim한 뒤 반환합니다.
 * - `.gz` extension file은 `GZIPInputStream`으로 자동 decompress합니다.
 * - Flow 기반 async loading API로 여러 dictionary path를 병렬로 collect할 수 있습니다.
 *
 * ```kotlin
 * val words = DictionaryProvider.readWordsAsSequence("dict/custom.txt").take(2).toList()
 * // words.size <= 2
 * ```
 */
object DictionaryProvider: KLogging() {

    private const val SPACE = " "
    private const val TAB = "\t"

    /**
     * [InputStream]을 line 단위로 읽고 trim한 string을 [Sequence]로 반환합니다.
     *
     * ## 동작 계약
     * - UTF-8로 decode하고 모든 line을 memory에 eager load합니다.
     * - Stream leak을 막기 위해 [Reader]를 `use {}`로 안전하게 닫습니다.
     * - 각 line은 `trim()`으로 변환합니다.
     *
     * @param stream 읽을 source [InputStream]입니다.
     * @return trim된 line sequence입니다.
     *
     * ```kotlin
     * val bytes = "a\n b ".byteInputStream()
     * val lines = DictionaryProvider.readStreamByLine(bytes).toList()
     * // lines == listOf("a", "b")
     * ```
     */
    fun readStreamByLine(stream: InputStream): Sequence<String> =
        InputStreamReader(stream, Charsets.UTF_8).buffered().use { reader ->
            reader.lineSequence().map { it.trim() }.toList()
        }.asSequence()

    /**
     * Classpath resource를 열고 line을 [Sequence]로 반환합니다.
     *
     * ## 동작 계약
     * - `classLoader.getResourceAsStream(path)`가 `null`을 반환하면 [IllegalStateException]을 던집니다.
     * - `.gz` file은 읽기 전에 decompress합니다.
     * - Plain text file은 raw stream에서 직접 읽습니다.
     *
     * @param path classpath resource path입니다.
     * @param classLoader resource stream을 찾을 [ClassLoader]입니다.
     * @return resource file의 trim된 line sequence입니다.
     *
     * ```kotlin
     * val lines = DictionaryProvider.readFileByLineFromResources("dict/words.txt")
     * // lines.first().isNotBlank() == true
     * ```
     */
    fun readFileByLineFromResources(
        path: String,
        classLoader: ClassLoader = Thread.currentThread().contextClassLoader,
    ): Sequence<String> {
        log.debug { "Read a file. path=$path" }

        val stream: InputStream? = classLoader.getResourceAsStream(path)
        check(stream != null) { "Can't open file. path=$path" }

        return if (path.endsWith(".gz")) {
            stream.use { s -> readStreamByLine(GZIPInputStream(s)) }
        } else {
            readStreamByLine(stream)
        }
    }

    /**
     * Word-frequency dictionary file을 읽어 `word -> frequency (Float)` map으로 적재합니다.
     *
     * ## 동작 계약
     * - Tab(`\t`)을 포함한 line만 처리합니다.
     * - Tab 뒤 첫 6글자를 [Float] frequency value로 parse합니다.
     * - 결과는 [destination]에 누적하며, duplicate key는 마지막 value로 overwrite합니다.
     *
     * @param path 읽을 word-frequency dictionary resource path입니다.
     * @param destination 결과를 누적할 mutable map입니다.
     * @return 누적된 `word -> frequency` map입니다. 반환 instance는 [destination]과 같습니다.
     *
     * ```kotlin
     * val map = DictionaryProvider.readWordFreqs("dict/freqs.txt", mutableMapOf())
     * // map.isNotEmpty() == true
     * ```
     */
    fun readWordFreqs(
        path: String,
        destination: MutableMap<CharSequence, Float> = mutableMapOf(),
    ): Map<CharSequence, Float> {
        val freqRange = 0 until 6
        // val map = ConcurrentHashMap<CharSequence, Float>()

        readFileByLineFromResources(path)
            .filter { it.contains(TAB) }
            .map {
                val elems = it.split(TAB, limit = 2)
                elems[0] to elems[1].slice(freqRange).toFloat()
            }
            .forEach {
                destination[it.first] = it.second
            }

        return destination
    }

    /**
     * Space-delimited two-column dictionary file을 읽고 word-mapping pair를 [Sequence]로 반환합니다.
     *
     * ## 동작 계약
     * - Space를 포함한 line만 처리합니다.
     * - 각 line은 첫 space에서 정확히 두 string으로 split합니다.
     *
     * @param filename 읽을 dictionary resource filename입니다.
     * @return `(sourceWord, mappedWord)` pair sequence입니다.
     *
     * ```kotlin
     * val pairs = DictionaryProvider.readWordMap("dict/map.txt").take(1).toList()
     * // pairs.size <= 1
     * ```
     */
    fun readWordMap(filename: String): Sequence<Pair<String, String>> {
        return readFileByLineFromResources(filename)
            .filter { it.contains(SPACE) }
            .map {
                val words = it.split(SPACE, limit = 2)
                words[0] to words[1]
            }
    }

    /**
     * Resource file의 line을 word [Sequence]로 반환합니다.
     *
     * ## 동작 계약
     * - [readFileByLineFromResources]에 직접 위임합니다.
     * - Sequence는 in-memory list를 감싸므로 file-handle leak이 없습니다.
     *
     * @param filename 읽을 word dictionary resource filename입니다.
     * @return trim된 word sequence입니다.
     *
     * ```kotlin
     * val first = DictionaryProvider.readWordsAsSequence("dict/words.txt").first()
     * // first.isNotBlank() == true
     * ```
     */
    fun readWordsAsSequence(filename: String): Sequence<String> {
        return readFileByLineFromResources(filename)
    }

    /**
     * 여러 resource file에서 word를 비동기로 읽고 [MutableSet]에 누적합니다.
     *
     * ## 동작 계약
     * - [paths]를 Flow로 순회하고 `async` extension으로 각 path를 비동기 변환합니다.
     * - Collect한 line은 `addAll`로 [destination]에 merge합니다.
     * - 누적 결과를 담은 같은 [destination] instance를 반환합니다.
     * - Blocking I/O는 [Dispatchers.IO]에서 수행합니다.
     *
     * @param paths 읽을 dictionary resource path 목록입니다.
     * @param destination word를 누적할 mutable set입니다.
     * @return 모든 path에서 읽은 word를 누적한 [destination] instance입니다.
     *
     * ```kotlin
     * val words = DictionaryProvider.readWordsAsSet("dict/a.txt", "dict/b.txt")
     * // words.isNotEmpty() == true
     * ```
     */
    suspend fun readWordsAsSet(
        vararg paths: String,
        destination: MutableSet<String> = mutableSetOf(),
    ): MutableSet<String> = withContext(Dispatchers.IO) {
        paths.asFlow()
            .async { path ->
                readFileByLineFromResources(path)
            }
            .collect { words ->
                destination.addAll(words)
            }

        destination
    }

    /**
     * 여러 resource file에서 word를 읽고 [CharArraySet]에 누적합니다.
     *
     * ## 동작 계약
     * - 각 path의 line sequence를 async Flow로 collect합니다.
     * - 결과는 `addAll`로 [destination]에 merge하며, duplicate word는 set semantic에 따라 deduplicate됩니다.
     * - 같은 [destination] instance를 반환합니다.
     * - Blocking I/O는 [Dispatchers.IO]에서 수행합니다.
     *
     * @param paths 읽을 dictionary resource path 목록입니다.
     * @param destination word를 누적할 [CharArraySet]입니다.
     * @return 모든 path에서 읽은 word를 누적한 [destination] instance입니다.
     *
     * ```kotlin
     * val set = DictionaryProvider.readWords("dict/stopwords.txt")
     * // set.size > 0
     * ```
     */
    suspend fun readWords(
        vararg paths: String,
        destination: CharArraySet = newCharArraySet(),
    ): CharArraySet = withContext(Dispatchers.IO) {
        paths.asFlow()
            .async { path ->
                readFileByLineFromResources(path)
            }
            .collect { words ->
                destination.addAll(words)
            }

        destination
    }

    /**
     * Dictionary loading에 맞는 size의 기본 [CharArraySet]을 만듭니다.
     *
     * ## 동작 계약
     * - Initial capacity는 5,000이며 dictionary loading 중 rehash 빈도를 줄입니다.
     *
     * @return dictionary word 적재에 사용할 비어 있는 [CharArraySet]입니다.
     *
     * ```kotlin
     * val set = DictionaryProvider.newCharArraySet()
     * // set.isEmpty() == true
     * ```
     */
    fun newCharArraySet(): CharArraySet {
        return CharArraySet(5_000)
    }
}
