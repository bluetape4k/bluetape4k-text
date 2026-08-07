# Tokenizer core library

`tokenizer-core` provides shared request and response models, policy options, dictionary loaders, compact character collections, and tokenizer exceptions. Use it directly when you build a boundary or custom processor; applications that only call Korean or Japanese processing normally receive it transitively.

## What it provides

- `TokenizeRequest`, `TokenizeResponse`, `BlockwordRequest`, and `BlockwordResponse`;
- locale and masking options plus `Severity.LOW`, `MIDDLE`, and `HIGH`;
- `MAX_TOKENIZE_TEXT_LENGTH` and `MAX_BLOCKWORD_TEXT_LENGTH`, both `100_000` in 0.3.0;
- `DictionaryProvider` for plain or gzip classpath dictionaries;
- `CharArraySet` and `CharArrayMap` for repeated character-sequence lookup;
- `TokenizerException` and `InvalidTokenizeRequestException`.

## Add the dependency

```kotlin
dependencies {
    implementation("io.github.bluetape4k.text:tokenizer-core:0.3.0")
}
```

Omit the explicit version when a bluetape4k BOM manages it.

## Smallest useful example

```kotlin
import io.bluetape4k.tokenizer.model.TokenizeOptions
import io.bluetape4k.tokenizer.model.tokenizeRequestOf
import java.util.Locale

val request = tokenizeRequestOf(
    text = "코틀린 코루틴",
    options = TokenizeOptions(locale = Locale.KOREAN),
)

println(request.text)
println(request.options.locale)
```

The factory validates the text before a processor receives it. It also creates the message metadata used by the shared contract.

## Dictionary loading

```kotlin
import io.bluetape4k.tokenizer.utils.DictionaryProvider

val words = DictionaryProvider.readWords("dict/base.txt", "dict/custom.txt")
println("blocked" in words)
```

`readWords` loads multiple resources through a coroutine-based asynchronous path and returns a `CharArraySet`. Call it during controlled setup and reuse the result. `readWordsAsSequence` is the lazy line-oriented alternative; `readWordFreqs` parses tab-separated word-frequency data.

## When to choose it

Use core models at an HTTP or message boundary, for shared dictionary tooling, or when implementing a processor. Choose [Korean](tokenizer-korean.md) or [Japanese](tokenizer-japanese.md) modules when you need actual morphological analysis.

## Constraints and failure behavior

Blank text and text longer than `100_000` characters are rejected before processing. Do not include the submitted text in an error response or log. `DictionaryProvider` expects classpath resources in its supported line formats; resource failures belong to setup and should not be retried on every request.

## Continue learning

- [Input safety](../guides/input-safety.md)
- [Dictionaries and blockwords](../guides/dictionaries-and-blockwords.md)
- [Tokenizer safety example](../examples/tokenizer-safety-examples.md)

## Source evidence

- [TokenizeRequest](https://github.com/bluetape4k/bluetape4k-text/blob/0.3.0/tokenizer-core/src/main/kotlin/io/bluetape4k/tokenizer/model/TokenizeRequest.kt)
- [BlockwordRequest](https://github.com/bluetape4k/bluetape4k-text/blob/0.3.0/tokenizer-core/src/main/kotlin/io/bluetape4k/tokenizer/model/BlockwordRequest.kt)
- [DictionaryProvider](https://github.com/bluetape4k/bluetape4k-text/blob/0.3.0/tokenizer-core/src/main/kotlin/io/bluetape4k/tokenizer/utils/DictionaryProvider.kt)

<!-- release-readme-diagrams:start -->
## Release diagrams {#release-diagrams}

These diagrams are loaded directly from README assets published with the `0.3.0` release and pinned to its immutable commit. They describe this manual's released structure and runtime flows, not later Snapshot changes. Select a preview to open the SVG at the same release commit.

### tokenizer core Class Structure diagram

[![tokenizer core Class Structure diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-text/aead213d2d25307d7d3684226943a5f95c7411f2/docs/images/readme-diagrams/tokenizer-core-class-01.png)](https://github.com/bluetape4k/bluetape4k-text/blob/aead213d2d25307d7d3684226943a5f95c7411f2/docs/images/readme-diagrams/tokenizer-core-class-01.svg)

_Release README: [`tokenizer-core/README.md`](https://github.com/bluetape4k/bluetape4k-text/blob/aead213d2d25307d7d3684226943a5f95c7411f2/tokenizer-core/README.md)_

<!-- release-readme-diagrams:end -->
