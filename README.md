# bluetape4k-text

[![CI](https://github.com/bluetape4k/bluetape4k-text/actions/workflows/ci.yml/badge.svg)](https://github.com/bluetape4k/bluetape4k-text/actions/workflows/ci.yml)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.bluetape4k.text/tokenizer-core)](https://central.sonatype.com/namespace/io.github.bluetape4k.text)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.3-7F52FF?logo=kotlin)](https://kotlinlang.org)
[![JVM](https://img.shields.io/badge/JVM-21-ED8B00?logo=openjdk)](https://openjdk.org)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

[한국어](./README.ko.md) | English

![Bluetape4k text processing workbench](./docs/assets/text-workbench.png)

Kotlin/JVM text processing library — Korean and Japanese morphological analyzers, multilingual language detection, and Aho-Corasick multi-keyword search — part of the bluetape4k ecosystem.

## Project Purpose

`bluetape4k-text` gives Kotlin services reusable text-processing primitives for
Korean/Japanese tokenization, language detection, dictionary-backed filtering,
and high-throughput keyword search.

## What It Provides

- **Tokenizer core** — shared request/response models, dictionary utilities,
  severity metadata, and `CharArraySet`.
- **Korean NLP** — normalization, POS tokenization, phrase extraction, stemming,
  sentence splitting, runtime dictionary extension, and blockword masking.
- **Japanese NLP** — Kuromoji IPAdic tokenization, POS helpers, noun filtering,
  and blockword masking.
- **Language detection** — Lingua-backed detector factories and mixed-language
  detection helpers.
- **Aho-Corasick search** — immutable automata, DSL builder, replacement, word
  boundary handling, and Kotlin Flow matching.

<!-- README_VISUAL_OVERVIEW:START -->
## Overview Diagram

![Bluetape4k Text overview diagram](docs/images/readme-diagrams/root-readme-overview-01.png)

## Module Composition Chart

![Bluetape4k Text module composition chart](docs/images/readme-diagrams/root-readme-module-chart-01.png)
<!-- README_VISUAL_OVERVIEW:END -->

## Modules

| Module | Description | Artifact |
|---|---|---|
| `bom` | Consumer BOM for aligned text artifacts | `io.github.bluetape4k.text:bluetape4k-text-bom` |
| `tokenizer-core` | Shared domain models (`TokenizeRequest/Response`, `BlockwordRequest/Response`, `Severity`), dictionary utilities (`DictionaryProvider`, `CharArraySet`) | `io.github.bluetape4k.text:tokenizer-core` |
| `lingua` | Kotlin DSL wrapper around Lingua — factory functions for `LanguageDetector`, mixed-language detection via `Set<Language>`, `UnicodeDetector` | `io.github.bluetape4k.text:lingua` |
| `text-search` | `AhoCorasickAutomaton<V>` — O(n+m+z) multi-keyword search, Unicode normalization, word boundaries, Kotlin Flow API | `io.github.bluetape4k.text:text-search` |
| `tokenizer-japanese` | `JapaneseProcessor` facade powered by Kuromoji IPAdic — morphological analysis, POS filtering, blockword detection and masking | `io.github.bluetape4k.text:tokenizer-japanese` |
| `tokenizer-korean` | `KoreanProcessor` facade — full Korean NLP pipeline: normalization, POS tokenization, phrase extraction, stemming, sentence splitting, blockword masking | `io.github.bluetape4k.text:tokenizer-korean` |

## Quality Evidence

The 0.2.0 quality gate is source-controlled and reproducible:

| Evidence | Link |
|---|---|
| Text quality gate and fixture corpus | [spec](docs/superpowers/specs/2026-05-27-issue-83-text-quality-benchmark-spec.md) |
| Dictionary and blockword update pipeline | [plan](docs/superpowers/plans/2026-05-27-issue-85-dictionary-update-pipeline-plan.md) |
| Tokenizer and language detection quality report | [report](docs/superpowers/research/2026-05-27-issue-86-quality-report.md) |

The report uses deterministic tests for mixed Korean/Japanese tokenization,
Lingua language detection, and sanitized request-boundary failures. It is a
release-readiness gate, not a broad statistical NLP benchmark claim.

## Architecture

![text Architecture diagram](docs/images/readme-diagrams/bluetape4k-text-architecture-01.png)

## Installation

Add each module individually:

```kotlin
// build.gradle.kts

// Korean NLP
implementation("io.github.bluetape4k.text:tokenizer-korean:0.1.0-SNAPSHOT")

// Japanese NLP
implementation("io.github.bluetape4k.text:tokenizer-japanese:0.1.0-SNAPSHOT")

// Language detection
implementation("io.github.bluetape4k.text:lingua:0.1.0-SNAPSHOT")

// Aho-Corasick search
implementation("io.github.bluetape4k.text:text-search:0.1.0-SNAPSHOT")

// Core models only (if you build a custom tokenizer)
implementation("io.github.bluetape4k.text:tokenizer-core:0.1.0-SNAPSHOT")
```

For snapshots, add the Maven Central Snapshots repository:

```kotlin
repositories {
    maven {
        url = uri("https://central.sonatype.com/repository/maven-snapshots/")
    }
}
```

## Usage

### Korean Tokenizer

```kotlin
import io.bluetape4k.tokenizer.korean.KoreanProcessor
import io.bluetape4k.tokenizer.model.BlockwordRequest
import io.bluetape4k.tokenizer.model.Severity

// 1. Normalize colloquial text
val normalized = KoreanProcessor.normalize("안됔ㅋㅋㅋㅋㅋ")
// → "안돼ㅋㅋㅋ"

// 2. Morphological tokenization
val tokens = KoreanProcessor.tokenize("주말특가 쇼핑몰")
KoreanProcessor.tokensToStrings(tokens)
// ["주말", "특가", "쇼핑몰"]

// 3. Stemming
val stemmed = KoreanProcessor.stem(KoreanProcessor.tokenize("가느다란"))
println(stemmed.first().stem)  // → "갈다"

// 4. Phrase extraction
val phrases = KoreanProcessor.extractPhrases(
    KoreanProcessor.tokenize("성탄절 쇼핑"),
    filterSpam = false
)

// 5. Sentence splitting
val sentences = KoreanProcessor.splitSentences("안녕? 세상아?").toList()
// size == 2

// 6. Runtime noun dictionary extension
KoreanProcessor.addNounsToDictionary("블루테이프4K", "주말특가")

// 7. Blockword masking
KoreanProcessor.addBlockwords(listOf("욕설"), Severity.HIGH)
val response = KoreanProcessor.maskBlockwords(BlockwordRequest("이 욕설은 나쁜 말이야"))
// response.maskedText → "이 **은 나쁜 말이야"
```

### Japanese Tokenizer

```kotlin
import io.bluetape4k.tokenizer.japanese.JapaneseProcessor
import io.bluetape4k.tokenizer.model.blockwordRequestOf

// Morphological analysis
val tokens = JapaneseProcessor.tokenize("お寿司が食べたい。")
val surfaces = tokens.map { it.surface }
// [お, 寿司, が, 食べ, たい, 。]

// Noun filtering
val nouns = JapaneseProcessor.filterNoun(
    JapaneseProcessor.tokenize("私は、日本語の勉強をしています。")
).map { it.surface }
// [私, 日本語, 勉強]

// Blockword masking
val request = blockwordRequestOf("ホモの男性を理解できない")
val result = JapaneseProcessor.maskBlockwords(request)
println(result.maskedText)       // **の男性を理解できない
println(result.blockwordExists)  // true
```

### Language Detection

```kotlin
import com.github.pemistahl.lingua.api.Language
import io.bluetape4k.lingua.allLanguageDetector
import io.bluetape4k.lingua.detectAllLanguagesOf

// Build a detector (reuse the instance — model loading is expensive)
val detector = allLanguageDetector {
    withPreloadedLanguageModels()
    withMinimumRelativeDistance(0.0)
}

// Single-language detection
val lang = detector.detectLanguageOf("Hello, world")
// Language.ENGLISH

// Mixed-language detection
val langs = detector.detectAllLanguagesOf("Hello 안녕 こんにちは")
// setOf(Language.ENGLISH, Language.KOREAN, Language.JAPANESE)

// Build from a specific language subset
val koEnDetector = languageDetectorOf(
    languages = setOf(Language.ENGLISH, Language.KOREAN),
    minimumRelativeDistance = 0.0,
    isEveryLanguageModelPreloaded = true
)
```

### Aho-Corasick Text Search

```kotlin
import io.bluetape4k.text.search.AhoCorasickAutomaton
import io.bluetape4k.text.search.SearchOptions
import io.bluetape4k.text.search.WordBoundary
import io.bluetape4k.text.search.ahoCorasick
import io.bluetape4k.text.search.flow.matchesAsFlow

// Builder API
val automaton = AhoCorasickAutomaton.builder<String>()
    .add("apple", "APPLE")
    .add("banana", "BANANA")
    .options(SearchOptions(ignoreCase = true, allowOverlaps = false))
    .build()

val matches = automaton.parseText("I like Apple and BANANA.")
// [Match(start=7, end=11, keyword="apple", value="APPLE"), ...]

// DSL builder
val kw = ahoCorasick<String> {
    ignoreCase = true
    wordBoundary = WordBoundary.LATIN_ALPHA
    keyword("fun", "KW_FUN")
    keyword("val", "KW_VAL")
}

// Profanity masking with replaceAll
val blocked = AhoCorasickAutomaton.builder<String>()
    .apply { listOf("bad", "worse").forEach { add(it, "***") } }
    .build()
val clean = blocked.replaceAll("That's bad and worse!") { it.value }
// "That's *** and ***!"

// Kotlin Flow — first alert
val firstAlert = automaton.matchesAsFlow("ERROR in disk")
    .take(1)
    .toList()
```

## Requirements

- **JDK**: 21+
- **Kotlin**: 2.3+
- **Gradle**: 8.x

## License

MIT License — see [LICENSE](LICENSE)
