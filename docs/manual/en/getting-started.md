# Getting started

This page takes you from dependency management to a first tokenization result. It uses the Korean processor because its output is easy to inspect, but the same version-management rules apply to every Text module.

## Requirements

- JDK 25 or newer
- Kotlin 2.4-compatible build
- a repository that can resolve Maven Central

## Prefer the ecosystem BOM

Applications that use more than one bluetape4k repository should import `bluetape4k-dependencies`. Users normally need to choose only that version; it coordinates Text with the rest of the Kotlin ecosystem.

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<release>"))
    implementation("io.github.bluetape4k.text:tokenizer-korean")
}
```

Use the Text-only BOM when your application manages the wider ecosystem separately or consumes only `io.github.bluetape4k.text:*` artifacts.

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k.text:bluetape4k-text-bom:0.3.0"))
    implementation("io.github.bluetape4k.text:tokenizer-korean")
}
```

The BOM supplies dependency constraints, not tokenizer classes. You must still add the runtime module you call.

## Run the smallest useful example

```kotlin
import io.bluetape4k.tokenizer.korean.KoreanProcessor

fun main() {
    val normalized = KoreanProcessor.normalize("안됔ㅋㅋㅋㅋㅋ")
    val tokens = KoreanProcessor.tokenize("주말특가 쇼핑몰")

    println(normalized)
    println(KoreanProcessor.tokensToStrings(tokens))
}
```

Expected result:

```text
안돼ㅋㅋㅋ
[주말, 특가, 쇼핑몰]
```

`normalize` repairs a known colloquial form and limits repeated laughter characters. `tokenize` performs morphological analysis; `tokensToStrings` is a presentation helper that exposes token text without discarding the richer token objects from the original result.

## Choose the next dependency

```kotlin
dependencies {
    implementation("io.github.bluetape4k.text:tokenizer-japanese")
    implementation("io.github.bluetape4k.text:lingua")
    implementation("io.github.bluetape4k.text:text-search")
}
```

Add only what the service uses. A language detector does not tokenize text, and a tokenizer is not a replacement for multi-keyword search. The [processing model](architecture/processing-model.md) shows how to combine them without turning every request into one mandatory pipeline.

## Before serving requests

Do not pass arbitrary HTTP input straight to a processor. Validate blank and oversized text at the boundary using the request contracts from `tokenizer-core`; map invalid input without echoing submitted text. See [input safety](guides/input-safety.md) and the [runnable safety example](examples/tokenizer-safety-examples.md).

Reuse configured detectors and immutable search automatons rather than rebuilding them for every call. See [startup and memory](operations/startup-and-memory.md).

## Source evidence

- [KoreanProcessor facade](https://github.com/bluetape4k/bluetape4k-text/blob/0.3.0/tokenizer-korean/src/main/kotlin/io/bluetape4k/tokenizer/korean/KoreanProcessor.kt)
- [Text BOM README](https://github.com/bluetape4k/bluetape4k-text/blob/0.3.0/bom/README.md)
- [Root installation examples](https://github.com/bluetape4k/bluetape4k-text/blob/0.3.0/README.md)
