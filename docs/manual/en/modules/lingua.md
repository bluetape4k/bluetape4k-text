# Language detection library

The `lingua` module is a Kotlin-friendly layer over the Lingua detector plus script-oriented Unicode helpers. Use it to estimate a language, collect likely languages from mixed text, or make a cheap script-based decision before loading a statistical model.

## What it provides

- detector factories for all languages, explicit `Language` sets, and ISO code sets;
- builder and parameter-based configuration;
- `detectAllLanguagesOf(text): Set<Language>` for mixed input;
- correction for ambiguous short Latin tokens;
- `UnicodeDetector` and character properties for Korean, Japanese, Chinese, Thai, and Latin scripts.

## Add the dependency

```kotlin
dependencies {
    implementation("io.github.bluetape4k.text:lingua:0.3.0")
}
```

The upstream Lingua engine is transitive.

## Smallest useful example

```kotlin
import com.github.pemistahl.lingua.api.Language
import io.bluetape4k.lingua.detectAllLanguagesOf
import io.bluetape4k.lingua.languageDetectorOf

val detector = languageDetectorOf(
    languages = setOf(Language.ENGLISH, Language.KOREAN, Language.JAPANESE),
    minimumRelativeDistance = 0.0,
    isEveryLanguageModelPreloaded = true,
    isLowAccuracyModeEnabled = false,
)

println(detector.detectAllLanguagesOf("Hello 안녕하세요 こんにちは"))
// [ENGLISH, KOREAN, JAPANESE] as a set; iteration order is not a contract
```

Build the detector once for the languages your application supports. Preloading favors predictable later calls at the cost of startup and memory.

## Script-first routing

```kotlin
import io.bluetape4k.lingua.UnicodeDetector
import java.util.Locale

val unicode = UnicodeDetector()
println(unicode.containsAny("Hello 안녕", Locale.KOREAN)) // true
println(unicode.filterString("Hello 안녕", Locale.KOREAN)) // [안, 녕]
```

Unicode filtering is deterministic but does not infer natural language. It is useful for a fast supported-script gate. Use statistical detection when Latin-language distinctions or ambiguous mixed text matter.

## When to choose it

Choose a restricted detector for a known set of supported languages. Use all-language detection only when the broader model set is genuinely required. Enable low-accuracy mode when its resource trade-off is acceptable and verify it with your text distribution.

## Constraints and failure behavior

Detection is evidence, not certainty. Short input and shared vocabulary can be ambiguous. Blank mixed-language input returns an empty set; when token-level detection yields no usable result, the extension falls back to whole-text detection. Define an unknown/ambiguous route instead of forcing every result into a processor.

## Continue learning

- [Language detection selection](../guides/mixed-language-processing.md)
- [Lingua runnable example](../examples/lingua-examples.md)
- [Runtime boundaries](../architecture/runtime-boundaries.md)

## Source evidence

- [Detector factories](https://github.com/bluetape4k/bluetape4k-text/blob/0.3.0/lingua/src/main/kotlin/io/bluetape4k/lingua/LanguageDetector.kt)
- [UnicodeDetector](https://github.com/bluetape4k/bluetape4k-text/blob/0.3.0/lingua/src/main/kotlin/io/bluetape4k/lingua/UnicodeDetector.kt)
- [Lingua example](https://github.com/bluetape4k/bluetape4k-text/blob/0.3.0/examples/lingua-examples/src/main/kotlin/io/bluetape4k/text/examples/lingua/LinguaExamples.kt)

<!-- release-readme-diagrams:start -->
## Release diagrams {#release-diagrams}

These diagrams are loaded directly from README assets published with the `0.3.0` release and pinned to its immutable commit. They describe this manual's released structure and runtime flows, not later Snapshot changes. Select a preview to open the SVG at the same release commit.

### lingua Architecture diagram

[![lingua Architecture diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-text/aead213d2d25307d7d3684226943a5f95c7411f2/docs/images/readme-diagrams/lingua-architecture-01.png)](https://github.com/bluetape4k/bluetape4k-text/blob/aead213d2d25307d7d3684226943a5f95c7411f2/docs/images/readme-diagrams/lingua-architecture-01.svg)

_Release README: [`lingua/README.md`](https://github.com/bluetape4k/bluetape4k-text/blob/aead213d2d25307d7d3684226943a5f95c7411f2/lingua/README.md)_

### lingua Class Structure 2 diagram

[![lingua Class Structure 2 diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-text/aead213d2d25307d7d3684226943a5f95c7411f2/docs/images/readme-diagrams/lingua-class-02.png)](https://github.com/bluetape4k/bluetape4k-text/blob/aead213d2d25307d7d3684226943a5f95c7411f2/docs/images/readme-diagrams/lingua-class-02.svg)

_Release README: [`lingua/README.md`](https://github.com/bluetape4k/bluetape4k-text/blob/aead213d2d25307d7d3684226943a5f95c7411f2/lingua/README.md)_

<!-- release-readme-diagrams:end -->
