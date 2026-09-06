[한국어](./README.ko.md) | English

# tokenizer-core

Core abstractions, domain models, and utilities for building text tokenizers and blockword processors in the bluetape4k ecosystem.

## Architecture

![tokenizer core Class Structure diagram](../docs/images/readme-diagrams/tokenizer-core-class-01.png)

## Features

- **Domain model layer** — `TokenizeRequest` / `TokenizeResponse` and `BlockwordRequest` / `BlockwordResponse` with automatic timestamp recording
- **Configurable options** — `TokenizeOptions` (locale) and `BlockwordOptions` (mask character, locale, severity level)
- **Severity enum** — `Severity.LOW` (slang/mild), `MIDDLE` (profanity), `HIGH` (hate speech / regional slurs) for fine-grained content policy
- **Dictionary utilities** — `DictionaryProvider` loads classpath resource files (plain text or `.gz`) as eager in-memory `Sequence` snapshots, word-frequency maps, or high-performance `CharArraySet`
- **Parallel dictionary loading** — `readWordsAsSet` and `readWords` use `Flow.async` to load multiple dictionary files concurrently
- **Success-only suspend memoization** — `SuspendMemoized` coalesces concurrent initialization, reuses successful values, retries after failure/cancellation, and restores the blocking caller's interruption flag
- **Efficient set/map structures** — `CharArraySet` and `CharArrayMap` optimised for high-throughput membership checks against large word lists
- **Exception hierarchy** — `TokenizerException` and `InvalidTokenizeRequestException` extend `BluetapeException`
- **Serializable models** — all options and message types implement `java.io.Serializable`

## Usage

### Tokenize request / response

```kotlin
import io.bluetape4k.tokenizer.model.*
import java.util.Locale

// Build a request with default options (Locale.KOREAN)
val request = tokenizeRequestOf("코틀린 코루틴")
println(request.text)       // 코틀린 코루틴
println(request.timestamp)  // epoch millis

// Build with custom locale
val jpOptions = TokenizeOptions(locale = Locale.JAPANESE)
val jpRequest = tokenizeRequestOf("日本語テスト", jpOptions)

// Construct a response (tokenizer implementations populate this)
val response = tokenizeResponseOf(request.text, listOf("코틀린", "코루틴"))
println(response.tokens)    // [코틀린, 코루틴]
```

### Blockword request / response

```kotlin
import io.bluetape4k.tokenizer.model.*

// Default: mask = "*", severity = LOW
val req = blockwordRequestOf("나쁜 단어가 포함된 문장")

// Custom mask character and severity
val opts = blockwordOptionsOf(mask = "#", severity = Severity.HIGH)
val req2 = blockwordRequestOf("some text", opts)

// Inspect a response built by a blockword processor
val response = blockwordResponseOf(req, "나쁜 ***가 포함된 문장", listOf("단어"))
println(response.blockwordExists)   // true
println(response.maskedText)        // 나쁜 ***가 포함된 문장
```

### Loading a dictionary from classpath resources

```kotlin
import io.bluetape4k.tokenizer.utils.DictionaryProvider
import kotlinx.coroutines.runBlocking

// Eager in-memory snapshot — the resource is fully read and closed before return
val words: Sequence<String> = DictionaryProvider.readWordsAsSequence("dict/stopwords.txt")

// Load multiple files in parallel into a CharArraySet
val set = runBlocking {
    DictionaryProvider.readWords("dict/a.txt", "dict/b.txt")
}
println(set.contains("foo"))  // true if "foo" is in either file

// Read a word-frequency file (tab-separated: word\tfrequency)
val freqMap: Map<CharSequence, Float> = DictionaryProvider.readWordFreqs("dict/freqs.txt")
```

`readWordsAsSequence` reads the complete resource before returning and closes its
stream. Calling `take(2)` limits sequence traversal after the read; it does not
reduce file I/O or peak memory. Use `readWordsAsSet` when you need deduplicated
membership lookup instead.

### Success-only suspend initialization

`SuspendMemoized` coalesces concurrent calls into one initialization and stores only
successful results. If the initializer fails or the calling coroutine is cancelled,
the result is not stored and the next call retries. Synchronous facades can use
`getBlocking()`; if the caller thread is interrupted while waiting, the method
propagates `InterruptedException` and restores the thread's interruption flag.

```kotlin
import io.bluetape4k.tokenizer.utils.SuspendMemoized
import kotlinx.coroutines.runBlocking

val dictionary = SuspendMemoized {
    mapOf("sample" to "value")
}

val value = runBlocking { dictionary.get() }
val sameValue = dictionary.getBlocking()
// value == sameValue
```

`clear()` explicitly removes a successful value so the next call runs the initializer
again. `isInitialized()` reports whether a successful value is currently published.


## Dependencies

| Dependency | Purpose |
|---|---|
| `bluetape4k-io` | Base I/O utilities and `BluetapeException` |
| `bluetape4k-coroutines` | `Flow.async` for parallel dictionary loading |
| `kotlinx-coroutines-core` | Coroutines runtime |

```kotlin
dependencies {
    implementation("io.github.bluetape4k.text:tokenizer-core:<current release or snapshot>")
}
```

## Web-Service Input Boundaries

`TokenizeRequest` and `BlockwordRequest` validate inputs before processor work:
blank text is invalid, `MAX_TOKENIZE_TEXT_LENGTH` caps tokenizer requests, and
`MAX_BLOCKWORD_TEXT_LENGTH` caps blockword requests. HTTP adapters should map
blank input to `400 Bad Request` and oversized input to `413 Payload Too Large`.
Return only status, actual length, and max length in error bodies.

```kotlin
import io.bluetape4k.tokenizer.model.MAX_BLOCKWORD_TEXT_LENGTH
import io.bluetape4k.tokenizer.model.MAX_TOKENIZE_TEXT_LENGTH
import io.bluetape4k.tokenizer.model.blockwordRequestOf
import io.bluetape4k.tokenizer.model.tokenizeRequestOf

fun tokenizeStatus(text: String): Int =
    when {
        text.isBlank() -> 400
        text.length > MAX_TOKENIZE_TEXT_LENGTH -> 413
        else -> {
            tokenizeRequestOf(text)
            200
        }
    }

fun blockwordStatus(text: String): Int =
    when {
        text.isBlank() -> 400
        text.length > MAX_BLOCKWORD_TEXT_LENGTH -> 413
        else -> {
            blockwordRequestOf(text)
            200
        }
    }
```

See [`../examples/tokenizer-safety-examples`](../examples/tokenizer-safety-examples)
for a runnable sample.
