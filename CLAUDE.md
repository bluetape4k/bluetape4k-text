# CLAUDE.md

Guidance for Claude Code when working in the `bluetape4k-text` repository.

## Project Overview

`bluetape4k-text` is a Kotlin/JVM text processing library that provides:

- **Korean morphological analysis** — normalization, POS tokenization, phrase extraction, stemming, sentence splitting, blockword masking
- **Japanese morphological analysis** — Kuromoji IPAdic-based tokenizer with POS filtering and blockword support
- **Multilingual language detection** — Kotlin DSL wrapper around Lingua, mixed-language detection via `Set<Language>`
- **Aho-Corasick multi-keyword search** — O(n+m+z) single-pass search with Unicode normalization, word boundaries, and Kotlin Flow support

Group: `io.github.bluetape4k.text` · Version: `0.1.0-SNAPSHOT` (see `gradle.properties`)

## Repository Layout

```
bluetape4k-text/
├── tokenizer-core/     # Shared domain models, options, dictionary utilities, CharArraySet/Map
├── tokenizer-japanese/ # JapaneseProcessor — Kuromoji IPAdic tokenizer + blockword masking
├── tokenizer-korean/   # KoreanProcessor — full Korean NLP pipeline (no twitter-text dependency)
├── lingua/             # LanguageDetector DSL, detectAllLanguagesOf(), UnicodeDetector
└── text-search/        # AhoCorasickAutomaton — immutable, thread-safe, generic-value Aho-Corasick
```

### Module descriptions

| Module | Artifact | Summary |
|---|---|---|
| `tokenizer-core` | `io.github.bluetape4k.text:tokenizer-core` | `TokenizeRequest/Response`, `BlockwordRequest/Response`, `Severity`, `DictionaryProvider`, `CharArraySet` |
| `tokenizer-japanese` | `io.github.bluetape4k.text:tokenizer-japanese` | `JapaneseProcessor` facade, Kuromoji IPAdic, POS extensions (`isNoun()`, `isVerb()`) |
| `tokenizer-korean` | `io.github.bluetape4k.text:tokenizer-korean` | `KoreanProcessor` facade, normalizer, chunker, POS tagger, phrase extractor, stemmer |
| `lingua` | `io.github.bluetape4k.text:lingua` | `allLanguageDetector {}`, `languageDetectorOf()`, `detectAllLanguagesOf()`, `UnicodeDetector` |
| `text-search` | `io.github.bluetape4k.text:text-search` | `AhoCorasickAutomaton<V>`, `ahoCorasick {}` DSL, `matchesAsFlow()`, `replaceAll()`, `tokenize()` |

## Build Commands

```bash
# Full build (skip tests)
./gradlew build -x test

# Full build with tests
./gradlew build

# Build a single module
./gradlew :tokenizer-korean:build
./gradlew :text-search:build

# Run tests for a single module
./gradlew :tokenizer-korean:test
./gradlew :text-search:test

# Run a specific test class
./gradlew :tokenizer-korean:test --tests "io.bluetape4k.tokenizer.korean.KoreanProcessorTest"

# Run all tests
./gradlew test

# Static analysis
./gradlew detekt

# Run JMH benchmarks (text-search only)
./gradlew :text-search:benchmark

# Publish snapshot to Maven Central (via nmcp)
./gradlew publishAllPublicationsToCentralPortal

# Publish to local Maven repository
./gradlew publishToMavenLocal
```

## Kotlin Edit Workflow (MANDATORY)

Before modifying a class, use `ide_find_references` or `get_impact_radius_tool` to identify affected files.

After every `.kt` edit:

1. Run `ide_diagnostics` — check import errors and `@Deprecated` warnings
2. Import errors → fix with `ide_optimize_imports`
3. `@Deprecated` → apply Quick Fix via `lsp_code_actions` — never leave unresolved
4. Build/compile only after passing the above steps

## Key Design Patterns

### Assert vs Require (CRITICAL — do NOT change exception types)

- `assertXxx()` → `AssertionError` (internal invariants, `@Deprecated`)
- `requireXxx()` → `IllegalArgumentException` (parameter validation — always use this)

Example from `AhoCorasickAutomaton.Builder.add`:
```kotlin
fun add(keyword: String, value: V): Builder<V> = apply {
    keyword.requireNotBlank("keyword")   // throws IllegalArgumentException if blank
    entries[keyword] = value
}
```

### Coroutines-First

All async work uses Kotlin Coroutines. Wrap blocking APIs with `withContext(Dispatchers.IO)`.

- `DictionaryProvider.readWords()` loads multiple files concurrently via `Flow.async`
- `AhoCorasickAutomaton.matchesAsFlow()` emits matches via `channelFlow + flowOn(Dispatchers.Default)`
- Never use `runBlocking` in production code paths; it is permitted only in `lazy` initializers for dictionary loading

### Facade pattern

`KoreanProcessor` and `JapaneseProcessor` are `object` facades that delegate to sub-components (`KoreanNormalizer`, `KoreanTokenizer`, `JapaneseBlockwordProcessor`, etc.). New operations go into the appropriate sub-component first, then get exposed through the facade.

### Immutable after build

`AhoCorasickAutomaton<V>` is immutable once built. The `Builder` holds all mutable state; calling `build()` produces a thread-safe read-only automaton. Never attempt to modify internal state after build.

### atomicfu scope

Use `atomicfu` at class-property level only — never for method-local variables.

### Virtual Threads

Never use `@Synchronized` / `synchronized {}`. Use `reentrantLock()` when shared-state locking is required.

## KDoc Requirements

All public classes, interfaces, object declarations, and extension functions must have KDoc. Include:

- A one-sentence summary
- A `## 동작/계약` section describing contracts and edge cases
- A ```` ```kotlin ```` usage example block

See `KoreanProcessor.kt` and `AhoCorasickAutomaton.kt` for reference.

## After Code Changes

- [ ] Run `ide_diagnostics` — zero errors, no unresolved `@Deprecated`
- [ ] Run compile + tests for the changed module: `./gradlew :<module>:test`
- [ ] Update `README.md` and `README.ko.md` for every changed module
- [ ] Add/update KDoc for all new or modified public APIs

## Before Creating a PR (MANDATORY)

- [ ] All module tests pass: `./gradlew :<module>:test` — report passing count + duration
- [ ] Code review: run `oh-my-claudecode:code-reviewer` — resolve all HIGH/CRITICAL issues before push
- [ ] `README.md` and `README.ko.md` updated for every changed module
- [ ] KDoc added/updated for all new or modified public APIs
- [ ] PR description includes test results, fix rationale, and verification commands
- [ ] Work was done in a git worktree (`.worktrees/<branch>`)

## Git Workflow

- Base branch: `develop`
- Commits: Korean prefix format (`feat: ...`, `fix: ...`, `docs: ...`, `refactor: ...`, `test: ...`, `chore: ...`)
- Worktree: `git worktree add .worktrees/<branch> -b <branch>`
- After merging PR: delete the worktree branch

## Build Configuration

- **JVM Toolchain**: Java 21
- **Kotlin**: 2.3 (language + API version)
- **Gradle flags**: ZGC daemon, parallel build, configuration cache enabled
- **Key compiler flags**: `-Xjsr305=strict`, `-jvm-default=enable`, `-Xcontext-parameters`, `-Xannotation-default-target=param-property`
- Dependency versions: `gradle/libs.versions.toml`
- Base version: `gradle.properties` (`baseVersion=0.1.0`)
