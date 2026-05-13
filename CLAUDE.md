# CLAUDE.md - bluetape4k-text

Text processing library for Korean/Japanese tokenization, multilingual language
detection, and Aho-Corasick search.

- **Group**: `io.github.bluetape4k.text`
- **Version**: `0.1.0-SNAPSHOT`

## Repository Layout

```text
tokenizer-core/      # Shared domain models, options, dictionary utilities, CharArraySet/Map
tokenizer-japanese/  # JapaneseProcessor; Kuromoji IPAdic tokenizer + blockword masking
tokenizer-korean/    # KoreanProcessor; full Korean NLP pipeline
lingua/              # LanguageDetector DSL, detectAllLanguagesOf(), UnicodeDetector
text-search/         # AhoCorasickAutomaton; immutable, thread-safe, generic-value
bom/                 # bluetape4k-text-bom consumer BOM
```

| Module | Artifact | Summary |
|---|---|---|
| `tokenizer-core` | `io.github.bluetape4k.text:tokenizer-core` | `TokenizeRequest/Response`, `BlockwordRequest/Response`, `Severity`, `DictionaryProvider`, `CharArraySet` |
| `tokenizer-japanese` | `io.github.bluetape4k.text:tokenizer-japanese` | `JapaneseProcessor`, Kuromoji IPAdic, POS extensions (`isNoun()`, `isVerb()`) |
| `tokenizer-korean` | `io.github.bluetape4k.text:tokenizer-korean` | `KoreanProcessor`, normalizer, chunker, POS tagger, phrase extractor, stemmer |
| `lingua` | `io.github.bluetape4k.text:lingua` | `allLanguageDetector {}`, `languageDetectorOf()`, `detectAllLanguagesOf()`, `UnicodeDetector` |
| `text-search` | `io.github.bluetape4k.text:text-search` | `AhoCorasickAutomaton<V>`, `ahoCorasick {}` DSL, `matchesAsFlow()`, `replaceAll()`, `tokenize()` |
| `bom/` | `io.github.bluetape4k.text:bluetape4k-text-bom` | Consumer BOM for aligned text artifacts |

## Build Commands

```bash
./gradlew build -x test
./gradlew build
./gradlew :tokenizer-korean:build
./gradlew :tokenizer-korean:test
./gradlew :tokenizer-korean:test --tests "io.bluetape4k.tokenizer.korean.KoreanProcessorTest"
./gradlew :text-search:benchmark
./gradlew detekt
./gradlew publishAllPublicationsToCentralPortal
./gradlew publishToMavenLocal
```

## Module Rules

- `KoreanProcessor` and `JapaneseProcessor` are facade objects. Add behavior to
  the right sub-component first, then expose it through the facade.
- `AhoCorasickAutomaton<V>` is immutable after `build()`. Only the builder is
  mutable.
- `DictionaryProvider.readWords()` loads multiple files in parallel through
  `Flow.async`; preserve coroutine semantics.
- `matchesAsFlow()` uses `channelFlow + flowOn(Dispatchers.Default)`.
- Use `KoreanProcessor.kt` and `AhoCorasickAutomaton.kt` as KDoc style examples.

## Documentation Rules

- Keep `README.md` and `README.ko.md` structurally aligned.
- Store shared README images under `docs/assets/` and reference them with the
  same relative path from both locales.
- Keep this file and other agent-facing guidance in English.
