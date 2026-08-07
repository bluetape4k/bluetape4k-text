# Korean tokenizer library

`tokenizer-korean` is the full Korean processing module. Its `KoreanProcessor` facade covers colloquial normalization, morphological tokenization, stemming, phrase extraction, sentence splitting, detokenization, runtime noun additions, and blockword masking.

## What it provides

- normalization for repetitions and known colloquial forms;
- 1-best and top-N tokenization with a 26-class POS model;
- noun-focused tokenization and phrase extraction;
- verb and adjective stems;
- sentence splitting and detokenization;
- pre-tokenization chunks such as URL, email, hashtag, screen name, number, Korean, alphabetic text, and punctuation;
- severity-layered blockword dictionaries and runtime noun updates.

## Add the dependency

```kotlin
dependencies {
    implementation("io.github.bluetape4k.text:tokenizer-korean:0.3.0")
}
```

## Smallest useful example

```kotlin
import io.bluetape4k.tokenizer.korean.KoreanProcessor

val normalized = KoreanProcessor.normalize("안됔ㅋㅋㅋㅋㅋ")
val tokens = KoreanProcessor.tokenize("주말특가 쇼핑몰")

println(normalized)                              // 안돼ㅋㅋㅋ
println(KoreanProcessor.tokensToStrings(tokens)) // [주말, 특가, 쇼핑몰]
```

Keep the structured `KoreanToken` values when later stages need POS, offsets, or stems. `tokensToStrings` is useful for display and simple assertions, not as a replacement for the analyzed result.

## Compose processing operations

```kotlin
val stemmed = KoreanProcessor.stem(KoreanProcessor.tokenize("가느다란"))
println(stemmed.first().stem) // 갈다

val phrases = KoreanProcessor.extractPhrases(
    KoreanProcessor.tokenize("성탄절 쇼핑"),
    filterSpam = false,
)

val sentences = KoreanProcessor.splitSentences("안녕? 세상아?").toList()
```

Normalization, tokenization, stemming, and phrase extraction are separate calls. Apply only the transformations your result contract needs; repeated analysis adds work and can discard information if reduced to strings too early.

## Runtime dictionaries and masking

```kotlin
import io.bluetape4k.tokenizer.model.BlockwordRequest
import io.bluetape4k.tokenizer.model.Severity

KoreanProcessor.addNounsToDictionary("블루테이프4K", "주말특가")
KoreanProcessor.addBlockwords(listOf("욕설"), Severity.HIGH)

val response = KoreanProcessor.maskBlockwords(BlockwordRequest("이 욕설은 나쁜 말이야"))
println(response.maskedText) // 이 **은 나쁜 말이야
```

Updates change in-process behavior. Authenticate and persist application-owned updates if several instances must share the same policy.

## When to choose it

Choose this module for Korean morphology or its built-in normalization and phrase operations. Use [text-search](text-search.md) instead when you only need exact multi-pattern matching. Use [Lingua](lingua.md) before this processor only when the service must route unknown-language input.

## Constraints and failure behavior

The facade rejects oversized inputs using the core limits. It is safe for concurrent use, but runtime dictionary mutation still changes shared policy. Warm the operations that matter to latency-sensitive services and avoid logging raw failed input.

## Continue learning

- [Mixed-language processing](../guides/mixed-language-processing.md)
- [Dictionaries and blockwords](../guides/dictionaries-and-blockwords.md)
- [Testing](../guides/testing.md)

## Source evidence

- [KoreanProcessor](https://github.com/bluetape4k/bluetape4k-text/blob/0.3.0/tokenizer-korean/src/main/kotlin/io/bluetape4k/tokenizer/korean/KoreanProcessor.kt)
- [Korean module README](https://github.com/bluetape4k/bluetape4k-text/blob/0.3.0/tokenizer-korean/README.md)
- [Korean processor tests](https://github.com/bluetape4k/bluetape4k-text/blob/0.3.0/tokenizer-korean/src/test/kotlin/io/bluetape4k/tokenizer/korean/KoreanProcessorTest.kt)

<!-- release-readme-diagrams:start -->
## Release diagrams {#release-diagrams}

These diagrams are loaded directly from README assets published with the `0.3.0` release and pinned to its immutable commit. They describe this manual's released structure and runtime flows, not later Snapshot changes. Select a preview to open the SVG at the same release commit.

### tokenizer korean Class Structure diagram

[![tokenizer korean Class Structure diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-text/aead213d2d25307d7d3684226943a5f95c7411f2/docs/images/readme-diagrams/tokenizer-korean-class-01.png)](https://github.com/bluetape4k/bluetape4k-text/blob/aead213d2d25307d7d3684226943a5f95c7411f2/docs/images/readme-diagrams/tokenizer-korean-class-01.svg)

_Release README: [`tokenizer-korean/README.md`](https://github.com/bluetape4k/bluetape4k-text/blob/aead213d2d25307d7d3684226943a5f95c7411f2/tokenizer-korean/README.md)_

<!-- release-readme-diagrams:end -->
