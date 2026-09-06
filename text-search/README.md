[한국어](./README.ko.md) | English

# text-search

Aho-Corasick multi-keyword search library for Kotlin/JVM. Searches N keywords simultaneously in O(n+m+z) time, with full support for Unicode normalization, word boundaries, case-insensitive matching, and Kotlin coroutines Flow API.

## Architecture

![text search Class Structure diagram](../docs/images/readme-diagrams/text-search-class-01.png)

### Search Pipeline

![Search Pipeline diagram](../docs/images/readme-diagrams/text-search-sequence-02.png)

### Processing Flow

![Processing Flow diagram](../docs/images/readme-diagrams/text-search-architecture-03.png)

## Features

| Feature | Description |
|---------|-------------|
| **O(n+m+z) search** | Searches N keywords in a single pass |
| **Generic values** | Associate any type `V` with each keyword |
| **Case-insensitive** | `SearchOptions(ignoreCase = true)` |
| **No overlaps** | `allowOverlaps = false` — longer keyword wins |
| **Word boundaries** | `LATIN_ALPHA` or `WHITESPACE_SEPARATED` |
| **Unicode NFC/NFKC** | Normalize before matching (with offset mapping) |
| **First match** | `firstMatch()` — leftmost-longest (R5 rule), regardless of `stopOnFirstMatch` |
| **Tokenize** | `tokenize()` — split into Match/Fragment tokens |
| **Replace** | `replaceAll(text) { match → replacement }` |
| **Keyword·regex redaction** | `TextRedactor` — original UTF-16 spans, overlap merge, same-length masking |
| **Flow API** | `matchesAsFlow(text)` — Kotlin coroutines Flow |
| **DSL builder** | `ahoCorasick { }` top-level function |
| **Thread-safe** | Immutable after build |

## Usage

### Basic Builder API

```kotlin
val automaton = AhoCorasickAutomaton.builder<String>()
    .add("apple", "APPLE")
    .add("banana", "BANANA")
    .add("cherry", "CHERRY")
    .options(SearchOptions(ignoreCase = true))
    .build()

val matches = automaton.parseText("I like Apple and BANANA.")
// [AhoCorasickMatch(start=7, end=11, keyword="apple", value="APPLE"),
//  AhoCorasickMatch(start=17, end=22, keyword="banana", value="BANANA")]
```

### DSL Builder

```kotlin
val automaton = ahoCorasick<String> {
    ignoreCase = true
    allowOverlaps = false
    keyword("apple", "APPLE")
    keyword("banana", "BANANA")
    keywords("cherry" to "CHERRY", "date" to "DATE")
}
```

### Simple Keyword Set

```kotlin
val automaton = ahoCorasickOf("apple", "banana", "cherry",
    options = SearchOptions(ignoreCase = true))
val found = automaton.containsMatch("I love apple pie")  // true
```

### Profanity Masking

```kotlin
val profanity = listOf("bad", "worse", "ugly")
val automaton = AhoCorasickAutomaton.builder<String>()
    .apply { profanity.forEach { add(it, "***") } }
    .build()

val masked = automaton.replaceAll("That's bad and worse!") { match -> match.value }
// "That's *** and ***!"
```

### Keyword·regex redaction

`TextRedactor` combines the existing Aho-Corasick matcher with optional regular-expression
rules behind one immutable policy. Overlapping matches are merged while adjacent spans stay
separate; ranges and masked output preserve the original UTF-16 offsets and length.

```kotlin
val redactor = TextRedactor.of(
    RedactionPolicy.of(
        rules = listOf(
            RedactionRule.keyword("keyword.account", "keyword", "account number", priority = 30),
            RedactionRule.regex("regex.email", "contact", "[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+", priority = 10),
        )
    )
)

val result = redactor.redact("account number: user@example.test")
// result.redactedText keeps the input length; result.spans use original half-open offsets.
```

`RedactionPolicy` defensively copies its rules and bounds input length and rule count.
`RedactionRule.toString()` and result metadata do not retain keyword or regex source text.
Regex rules are intended for trusted inputs; this API does not claim general ReDoS protection.
The redactor does not guarantee that undetected sensitive data is absent, so domain-specific
validation or a dedicated DLP policy remains the caller's responsibility.

### Code Syntax Highlight (Tokenize)

```kotlin
val keywords = listOf("fun", "val", "var", "class", "return", "if", "for")
val automaton = ahoCorasick<String> {
    wordBoundary = WordBoundary.LATIN_ALPHA
    keywords.forEach { keyword(it, it) }
}

val tokens = automaton.tokenize("fun greet() { val name = \"world\"; return name }")
val html = buildString {
    tokens.forEach { token ->
        when (token) {
            is SearchToken.Match    -> append("<b>${token.text}</b>")
            is SearchToken.Fragment -> append(token.text)
        }
    }
}
// "<b>fun</b> greet() { <b>val</b> name = \"world\"; <b>return</b> name }"
```

### Flow API — First Alert

```kotlin
val automaton = AhoCorasickAutomaton.builder<String>()
    .add("ERROR", "ALERT_ERROR")
    .add("WARN", "ALERT_WARN")
    .add("FATAL", "ALERT_FATAL")
    .build()

val logLine = "2026-04-26 INFO Starting... WARN disk low ERROR disk full"

val firstAlert = automaton.matchesAsFlow(logLine)
    .take(1)
    .toList()
// [AhoCorasickMatch(keyword="WARN", value="ALERT_WARN")]
```

### Unicode Korean Normalization

```kotlin
val automaton = AhoCorasickAutomaton.builder<String>()
    .add("나라", "COUNTRY")
    .options(SearchOptions(normalization = NormalizationForm.NFC))
    .build()

val result = automaton.parseText("아름다운 나라")
// matches "나라" even when input uses decomposed Jamo
```

### Normalized Keyword Collisions

The builder applies the same Unicode normalization and case-folding pipeline
to registered keywords and searched text. If two distinct source keywords
produce the same normalized key, `build()` throws `IllegalArgumentException`
instead of silently replacing one value. Register one canonical spelling or
use distinct normalized keys.

## API Reference

### `AhoCorasickAutomaton<V>`

| Method | Description |
|--------|-------------|
| `parseText(text)` | Returns all matches in the text |
| `firstMatch(text)` | Returns the leftmost-longest match (R5 rule); always evaluates all candidates even when `stopOnFirstMatch = true` |
| `containsMatch(text)` | Returns `true` if any keyword matches the configured word boundary; short-circuits after an accepted match |
| `tokenize(text)` | Splits into `Match` and `Fragment` tokens; always returns non-overlapping sequence |
| `replaceAll(text) { }` | Replaces all matches via transform lambda |

### Redaction API

| Type/method | Description |
|--------|-------------|
| `RedactionRule.keyword/regex` | Keyword or regex rule with safe metadata and priority |
| `RedactionPolicy.of` | Immutable rule snapshot, mask character, input/rule bounds, and keyword normalization |
| `TextRedactor.redact` | Returns a `RedactionResult` after overlap merging and same-length masking |
| `RedactionSpan` | Original UTF-16 half-open range, priority-ordered rule ids, and representative category |

Lower priority values win; ties are resolved by rule id and category. Adjacent spans are not
merged. `redactedText` is not a guarantee that every sensitive value was removed.

### `SearchOptions`

| Field | Default | Description |
|-------|---------|-------------|
| `ignoreCase` | `false` | Case-insensitive matching |
| `allowOverlaps` | `true` | Allow overlapping matches |
| `wordBoundary` | `NONE` | Word boundary detection mode |
| `normalization` | `NONE` | Unicode normalization form |
| `stopOnFirstMatch` | `false` | Stop `parseText` after its first match; does not limit `firstMatch` and is ignored in `matchesAsFlow` |

### `WordBoundary`

| Value | Description |
|-------|-------------|
| `NONE` | Substring matching (no boundary check) |
| `LATIN_ALPHA` | Alphabetic word boundaries (`Character.isAlphabetic`) |
| `WHITESPACE_SEPARATED` | Whitespace-separated token boundaries |

### Flow Extension

```kotlin
// matchesAsFlow runs on Dispatchers.Default via channelFlow
fun <V> AhoCorasickAutomaton<V>.matchesAsFlow(text: CharSequence): Flow<AhoCorasickMatch<V>>
```

## Runnable Example

See [`../examples/text-search-examples`](../examples/text-search-examples) for a
console sample covering the builder API, `ahoCorasick` DSL, replacement, and
`matchesAsFlow(...).take(1)` early collection for alert-style search.

## Benchmark

Throughput is measured with JMH through the repo-local kotlinx-benchmark task.
Higher `ops/s` is better. The 0.2.1 baseline covers large dictionaries, dense
matches, no-match input, Unicode normalization, and Flow collection.

Run condition:

- Command: `./gradlew :text-search:benchmark`
- Host: Apple M4 Pro, 48 GiB memory
- JVM from benchmark JSON: GraalVM JDK 21.0.11
- Raw result: [`docs/benchmark/2026-06-04-issue-97-ahocorasick-baselines.json`](../docs/benchmark/2026-06-04-issue-97-ahocorasick-baselines.json)

| Benchmark | Ops/s | Notes |
|-----------|-------|-------|
| `parseTextNoMatch` | 12,209.23 | 5,000-keyword automaton, no matches |
| `parseTextDenseMatches` | 3,566.90 | Overlapping dense matches |
| `parseTextLargeDictionary` | 3,116.99 | 5,000 keywords, 2,000 matched tokens |
| `matchesAsFlowLargeDictionaryCollect` | 712.62 | Flow collection over the large-dictionary input |
| `naiveContainsSmallDictionary` | 248.39 | 1,000-keyword sequential `String.contains` baseline |
| `parseTextNfkcNormalization` | 3.68 | NFKC + ignore-case normalization path |

> These are local comparable snapshots, not production rankings. Keep future
> runs on the same command and metric direction before comparing deltas.

#### VersionedDictionary mutation benchmark (Issue #239)

The dictionary benchmark calls the Korean production provider directly and
compares the changed-entry copy-on-write path with full replacement. Each
invocation performs an add/remove pair on the Noun-sized dictionary, restores
the same cardinality, and normalizes throughput over two mutations.

Run condition:

- Command: `./gradlew :text-search:dictionaryBenchmark`
- Host/JVM: Apple M4 Pro, GraalVM JDK 25.0.4, one JMH thread, one fork
- Raw result: [`docs/benchmark/2026-08-11-issue-239-versioned-dictionary-baselines.json`](../docs/benchmark/2026-08-11-issue-239-versioned-dictionary-baselines.json)

| Benchmark | Ops/s | Notes |
|-----------|-------|-------|
| `addRemoveWithCopyOnWrite` | 63.85 ± 29.30 | Production changed-entry COW path |
| `addRemoveWithFullReplacement` | 43.00 ± 36.52 | Production full-replacement path |

The confidence intervals overlap, so this run does not establish a
statistically conclusive throughput improvement. The raw JSON contains no
`gc.alloc.rate.norm` secondary metric; allocation/heap-retention improvement
is therefore not claimed from this run. The deterministic bounded-history
tests remain the retention proof.

#### Dictionary preload timing diagnostic (Issue #262)

The preload timing diagnostic runs in a separate `JavaExec` JVM. For each
provider, the first `preload()` call is recorded as `coldMs` and the second
call in the same process as `warmMs`. It is a reproducible observation task,
not a JMH throughput benchmark or a performance acceptance threshold.

Run it with:

```bash
./gradlew :text-search:preloadTimingDiagnostic --no-build-cache --rerun-tasks --console=plain
```

The local raw result and environment are preserved in
[`docs/benchmark/2026-08-12-issue-262-preload-timing.json`](../docs/benchmark/2026-08-12-issue-262-preload-timing.json).
Compare cold/warm observations only when the command, JVM, host class, and
metric direction remain compatible.

Run benchmarks locally:

```bash
./gradlew :text-search:benchmark
./gradlew :text-search:dictionaryBenchmark
```

## Dependencies

| Dependency | Purpose |
|---|---|
| `bluetape4k-core` | Core utilities |
| `kotlinx-coroutines-core` | Public Flow API (transitive `api`) |

```kotlin
// build.gradle.kts
implementation("io.github.bluetape4k.text:text-search:<current release or snapshot>")

// text-search publishes kotlinx-coroutines-core transitively through its public API
```
