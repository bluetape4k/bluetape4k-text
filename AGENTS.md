# AGENTS.md - bluetape4k-text

## Guidance hierarchy

Before applying this repository overlay, read and follow the guidance in this
order:

1. User scope: `${CODEX_HOME:-$HOME/.codex}/AGENTS.md`.
2. Workspace scope: `/Users/debop/work/bluetape4k/.github/docs/workspace/AGENTS.md`.

Apply both broader scopes before repository-specific rules.

This repository inherits the workspace guidance from `../AGENTS.md`.
Read and follow the workspace root guide first. This file only adds
repo-specific layout, commands, domain rules, and local exceptions.


Text processing library for Korean/Japanese tokenization, language detection,
and Aho-Corasick search.

- Group: `io.github.bluetape4k.text`
- Version: `0.1.0-SNAPSHOT`

## Modules

| Module | Artifact | Purpose |
|---|---|---|
| `tokenizer-core` | `io.github.bluetape4k.text:tokenizer-core` | Shared domain models, options, dictionary utilities, `CharArraySet` |
| `tokenizer-japanese` | `io.github.bluetape4k.text:tokenizer-japanese` | `JapaneseProcessor`, Kuromoji IPAdic, POS helpers |
| `tokenizer-korean` | `io.github.bluetape4k.text:tokenizer-korean` | `KoreanProcessor`, normalizer, chunker, POS tagger, phrase extractor, stemmer |
| `lingua` | `io.github.bluetape4k.text:lingua` | Language detector DSL and Unicode detector |
| `text-search` | `io.github.bluetape4k.text:text-search` | Immutable generic `AhoCorasickAutomaton<V>` and DSL |
| `bom/` | `io.github.bluetape4k.text:bluetape4k-text-bom` | Consumer BOM for aligned text artifacts |

## Commands

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

## Rules

- `KoreanProcessor` and `JapaneseProcessor` are facade objects. Add behavior to
  the right sub-component first, then expose it through the facade.
- `AhoCorasickAutomaton<V>` is immutable after `build()`. Only the builder is
  mutable.
- `DictionaryProvider.readWords()` loads multiple files in parallel through
  `Flow.async`; preserve coroutine semantics.
- `matchesAsFlow()` uses `channelFlow + flowOn(Dispatchers.Default)`.
- Use `KoreanProcessor.kt` and `AhoCorasickAutomaton.kt` as KDoc style examples.

## Repo-Specific Guards

- For tokenizer/search module changes, keep benchmark evidence and BOM/catalog
  constraints synchronized with the changed module.
- Preserve coroutine semantics when adding performance or benchmark coverage.
