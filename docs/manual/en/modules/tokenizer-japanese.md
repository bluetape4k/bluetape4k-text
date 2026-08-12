# Japanese tokenizer library

`tokenizer-japanese` wraps Kuromoji IPAdic behind `JapaneseProcessor`. It provides morphological tokenization, POS predicates and filters, compound-aware blockword detection, masking, and runtime dictionary management.

## What it provides

- Kuromoji IPAdic tokenization into `TokenBase` values;
- `filterNoun` and predicate-based `filter`;
- `isNoun`, `isVerb`, `isNounOrVerb`, `isAdjective`, `isJosa`, and `isPunctuation` helpers;
- noun/verb blockword detection, including adjacent compound checks;
- add, remove, and clear operations for the in-memory blockword dictionary.

## Add the dependency

```kotlin
dependencies {
    implementation("io.github.bluetape4k.text:tokenizer-japanese:0.3.0")
}
```

## Smallest useful example

```kotlin
import io.bluetape4k.tokenizer.japanese.JapaneseProcessor

val tokens = JapaneseProcessor.tokenize("お寿司が食べたい。")
println(tokens.map { it.surface })
// [お, 寿司, が, 食べ, たい, 。]
```

The returned tokens retain dictionary and POS details. Use `surface` only when the displayed form is enough.

## Filter by part of speech

```kotlin
import io.bluetape4k.tokenizer.japanese.tokenizer.isVerb

val analyzed = JapaneseProcessor.tokenize("私は、日本語の勉強をしています。")
val nouns = JapaneseProcessor.filterNoun(analyzed).map { it.surface }
val verbs = JapaneseProcessor.filter(analyzed) { it.isVerb() }.map { it.surface }

println(nouns) // [私, 日本語, 勉強]
println(verbs) // [し]
```

POS filtering is morphology-aware. It is more appropriate than raw substring search when the policy depends on grammatical category.

## Detect and mask blockwords

```kotlin
import io.bluetape4k.tokenizer.model.blockwordOptionsOf
import io.bluetape4k.tokenizer.model.blockwordRequestOf
import java.util.Locale

val options = blockwordOptionsOf(locale = Locale.JAPANESE)
val response = JapaneseProcessor.maskBlockwords(
    blockwordRequestOf("ホモの男性を理解できない", options),
)

println(response.maskedText)       // **の男性を理解できない
println(response.blockwordExists)  // true
```

The built-in path targets noun and verb tokens and also checks adjacent noun plus noun/verb compounds when a single token does not match.
`Locale.JAPANESE` is required. Severity uses cumulative thresholds: `LOW` includes all tiers, `MIDDLE` includes middle/high, and `HIGH` includes only high-tier entries.

## Dictionary lifetime

The packaged dictionary loads lazily on first blockword access. `addBlockwords`, `removeBlockwords`, and `clearBlockwords` mutate the process-wide in-memory policy. Warm the first access when latency matters, and restore application updates after restart.

## When to choose it

Choose this module when Japanese token boundaries or POS matter. Choose [text-search](text-search.md) for exact patterns independent of morphology. Route unknown-language input through [Lingua](lingua.md) only when routing is required.

## Constraints and failure behavior

The facade enforces core request length limits. Kuromoji dictionary loading and the first blockword dictionary access contribute startup work. Treat processor failures as sanitized internal errors rather than returning the original text.

## Continue learning

- [Mixed-language processing](../guides/mixed-language-processing.md)
- [Tokenizer safety example](../examples/tokenizer-safety-examples.md)
- [Startup and memory](../operations/startup-and-memory.md)

## Source evidence

- [JapaneseProcessor](https://github.com/bluetape4k/bluetape4k-text/blob/0.3.0/tokenizer-japanese/src/main/kotlin/io/bluetape4k/tokenizer/japanese/JapaneseProcessor.kt)
- [Japanese module README](https://github.com/bluetape4k/bluetape4k-text/blob/0.3.0/tokenizer-japanese/README.md)
- [Japanese processor tests](https://github.com/bluetape4k/bluetape4k-text/blob/0.3.0/tokenizer-japanese/src/test/kotlin/io/bluetape4k/tokenizer/japanese/JapaneseProcessorTest.kt)

<!-- release-readme-diagrams:start -->
## Release diagrams {#release-diagrams}

These diagrams are loaded directly from README assets published with the `0.3.0` release and pinned to its immutable commit. They describe this manual's released structure and runtime flows, not later Snapshot changes. Select a preview to open the SVG at the same release commit.

### tokenizer japanese Class Structure diagram

[![tokenizer japanese Class Structure diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-text/aead213d2d25307d7d3684226943a5f95c7411f2/docs/images/readme-diagrams/tokenizer-japanese-class-01.png)](https://github.com/bluetape4k/bluetape4k-text/blob/aead213d2d25307d7d3684226943a5f95c7411f2/docs/images/readme-diagrams/tokenizer-japanese-class-01.svg)

_Release README: [`tokenizer-japanese/README.md`](https://github.com/bluetape4k/bluetape4k-text/blob/aead213d2d25307d7d3684226943a5f95c7411f2/tokenizer-japanese/README.md)_

<!-- release-readme-diagrams:end -->
