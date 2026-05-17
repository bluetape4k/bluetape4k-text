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
 * Utility that reads classpath resource dictionary files and converts them into tokenizer input structures.
 *
 * ## Behavior / Contract
 * - Lines are read as UTF-8 and trimmed before being returned.
 * - Files with a `.gz` extension are decompressed automatically via `GZIPInputStream`.
 * - A Flow-based async loading API allows multiple dictionary paths to be collected in parallel.
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
     * Reads an [InputStream] line by line and returns trimmed strings as a [Sequence].
     *
     * ## Behavior / Contract
     * - Decodes as UTF-8 and eagerly loads all lines into memory.
     * - Closes the [Reader] safely via `use {}` to prevent stream leaks.
     * - Each line is transformed with `trim()`.
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
     * Opens a classpath resource and returns its lines as a [Sequence].
     *
     * ## Behavior / Contract
     * - Throws [IllegalStateException] when `classLoader.getResourceAsStream(path)` returns `null`.
     * - `.gz` files are decompressed before reading.
     * - Plain text files are read directly from the raw stream.
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
     * Reads a word-frequency dictionary file and loads it into a `word -> frequency (Float)` map.
     *
     * ## Behavior / Contract
     * - Only lines containing a tab (`\t`) are processed.
     * - The first 6 characters after the tab are parsed as a [Float] frequency value.
     * - Results are accumulated in [destination]; duplicate keys are overwritten with the last value.
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
     * Reads a two-column space-delimited dictionary file and returns word-mapping pairs as a [Sequence].
     *
     * ## Behavior / Contract
     * - Only lines containing a space are processed.
     * - Each line is split at the first space into exactly two strings.
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
     * Returns the lines of a resource file as a word [Sequence].
     *
     * ## Behavior / Contract
     * - Delegates directly to [readFileByLineFromResources].
     * - The sequence wraps an in-memory list, so there is no file-handle leak.
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
     * Asynchronously reads words from multiple resource files and accumulates them into a [MutableSet].
     *
     * ## Behavior / Contract
     * - Iterates [paths] as a Flow and transforms each path asynchronously via the `async` extension.
     * - Collected lines are merged into [destination] via `addAll`.
     * - Returns the same [destination] instance with all accumulated results.
     * - Blocking I/O is performed on [Dispatchers.IO].
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
     * Reads words from multiple resource files and accumulates them into a [CharArraySet].
     *
     * ## Behavior / Contract
     * - Collects each path's line sequence via an async Flow.
     * - Results are merged into [destination] via `addAll`; duplicate words are deduplicated by set semantics.
     * - Returns the same [destination] instance.
     * - Blocking I/O is performed on [Dispatchers.IO].
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
     * Creates a default [CharArraySet] sized for dictionary loading.
     *
     * ## Behavior / Contract
     * - Initial capacity is 5,000 to reduce rehash frequency during dictionary loading.
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
