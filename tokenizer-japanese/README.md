[한국어](./README.ko.md) | English

# tokenizer-japanese

Japanese morphological analysis and blockword filtering library powered by Kuromoji IPAdic, built on top of `tokenizer-core`.

## Architecture

![tokenizer japanese Class Structure diagram](../docs/images/readme-diagrams/tokenizer-japanese-class-01.png)

## Features

- **Morphological analysis** — tokenizes Japanese text into morphemes using Kuromoji IPAdic dictionary
- **Part-of-speech filtering** — built-in `filterNoun` plus a generic `filter` accepting any predicate
- **POS extension functions** — `isNoun()`, `isVerb()`, `isNounOrVerb()`, `isAdjective()`, `isJosa()`, `isPunctuation()` on `TokenBase`
- **Blockword detection** — finds blocked words from a pre-loaded `CharArraySet` dictionary; targets nouns and verbs only
- **Compound word detection** — when no single-token match is found, checks adjacent noun + noun/verb pairs (e.g. 覚せい剤 → 覚せい + 剤)
- **Blockword masking** — validates `Locale.JAPANESE`, applies LOW/MIDDLE/HIGH cumulative severity thresholds, and replaces each blocked token with the mask character repeated to match token length
- **Dynamic dictionary management** — add, remove, or clear blockwords at runtime without restarting the application
- **Suspend dictionary preload** — `JapaneseProcessor.preload()` loads the blockword dictionary from `japanesetext/block/blocks.txt` plus severity overrides before readiness; direct synchronous access remains a compatibility fallback
- **Facade pattern** — `JapaneseProcessor` provides a single entry point delegating to all sub-components

## Usage

### Startup dictionary preload

Call the facade preload from an application startup coroutine so the first
blockword request does not perform dictionary I/O on the request thread.

```kotlin
import io.bluetape4k.tokenizer.japanese.JapaneseProcessor

suspend fun warmUpJapaneseTokenizer() {
    JapaneseProcessor.preload()
}
```

### Morphological analysis

```kotlin
import io.bluetape4k.tokenizer.japanese.JapaneseProcessor

val tokens = JapaneseProcessor.tokenize("お寿司が食べたい。")
val surfaces = tokens.map { it.surface }
// [お, 寿司, が, 食べ, たい, 。]
```

### Part-of-speech filtering

```kotlin
import io.bluetape4k.tokenizer.japanese.JapaneseProcessor
import io.bluetape4k.tokenizer.japanese.tokenizer.isVerb

val tokens = JapaneseProcessor.tokenize("私は、日本語の勉強をしています。")

// Built-in noun filter
val nouns = JapaneseProcessor.filterNoun(tokens).map { it.surface }
// [私, 日本語, 勉強]

// Custom predicate — verbs only
val verbs = JapaneseProcessor.filter(tokens) { it.isVerb() }.map { it.surface }
// [し]
```

### Blockword detection and masking

```kotlin
import io.bluetape4k.tokenizer.japanese.JapaneseProcessor
import io.bluetape4k.tokenizer.model.blockwordRequestOf
import io.bluetape4k.tokenizer.model.blockwordOptionsOf
import io.bluetape4k.tokenizer.model.Severity
import java.util.Locale

// Detect blockwords
val found = JapaneseProcessor.findBlockwords("ホモの男性を理解できない").map { it.surface }
// [ホモ]

// Mask Japanese blockwords with an explicit locale and severity threshold
val options = blockwordOptionsOf(locale = Locale.JAPANESE, severity = Severity.MIDDLE)
val request = blockwordRequestOf("ホモの男性を理解できない", options)
val response = JapaneseProcessor.maskBlockwords(request)
println(response.maskedText)        // **の男性を理解できない
println(response.blockwordExists)   // true
println(response.blockWords)        // [ホモ]
```

`maskBlockwords` requires `Locale.JAPANESE`. Severity uses cumulative thresholds:
`LOW` includes all tiers, `MIDDLE` includes middle/high, and `HIGH` includes
only high-tier entries. Runtime words added without a tier use `LOW`.

### Dynamic dictionary management

```kotlin
import io.bluetape4k.tokenizer.japanese.JapaneseProcessor

// Add custom blockwords at runtime
JapaneseProcessor.addBlockwords(listOf("東京", "大阪"))
val found = JapaneseProcessor.findBlockwords("これは東京です").map { it.surface }
// [東京]

// Remove a word from the dictionary
JapaneseProcessor.removeBlockwords(listOf("東京"))

// Clear the entire in-memory dictionary
JapaneseProcessor.clearBlockwords()
```

## Dependencies

| Dependency | Purpose |
|---|---|
| `tokenizer-core` | Domain models, `DictionaryProvider`, `CharArraySet` |
| `bluetape4k-coroutines` | Async dictionary loading |
| `kuromoji-ipadic` | Kuromoji IPAdic morphological analyzer |

```kotlin
dependencies {
    implementation("io.github.bluetape4k.text:tokenizer-japanese:<current release or snapshot>")
}
```

## Web-Service Input Boundaries

Use `tokenizeRequestOf` and `blockwordRequestOf` at HTTP boundaries before
calling `JapaneseProcessor`. Blank input should map to `400 Bad Request`; text
longer than `MAX_TOKENIZE_TEXT_LENGTH` or `MAX_BLOCKWORD_TEXT_LENGTH` should map
to `413 Payload Too Large`. Error bodies should include only status and length
metadata, not the submitted text.

See [`../examples/tokenizer-safety-examples`](../examples/tokenizer-safety-examples)
for a runnable Korean/Japanese safety sample.

> Internally uses [Kuromoji IPAdic](https://github.com/atilika/kuromoji) for Japanese morphological analysis.
