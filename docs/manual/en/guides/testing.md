# Testing

Text-processing tests should lock user-visible contracts without freezing every internal morphological decision. Separate boundary, route, token, policy, search, and performance assertions.

## Request boundary tests

For both tokenize and blockword requests, cover:

- blank and whitespace-only text;
- exactly `100_000` characters;
- `100_001` characters;
- sanitized error messages that omit a sentinel from the submitted text.

Test the adapter status mapping separately from the model exception. This shows whether a failure is caused by transport policy or the library guard.

## Korean and Japanese fixtures

Assert stable surface tokens for representative mixed text. Add POS or stem assertions only when the product depends on them. The release quality gate intentionally avoids coupling every test to all internal morphological choices.

For Korean, include normalization, a runtime noun, a stem, sentence splitting, and a blockword severity. For Japanese, include noun and verb filters, a compound blockword, a runtime dictionary update, and masking.

## Language detection tests

Build a detector for the product's supported set and reuse it in the fixture. Cover:

- one clear sample per supported language;
- mixed Korean/Japanese/Latin text;
- short ambiguous Latin text;
- blank input;
- route policy for an empty or multi-language set.

Assert the set and application route separately. Set iteration order is not a contract.

## Aho-Corasick tests

```kotlin
val automaton = ahoCorasick<String> {
    allowOverlaps = false
    keyword("he", "HE")
    keyword("hers", "HERS")
}

val values = automaton.parseText("hers").map { it.value }
```

Cover overlapping keywords, no-match input, case-insensitive matching, each boundary mode, NFC/NFKC offset mapping, replacement, and `take(1)` Flow collection. Test immutable snapshot replacement by building a new automaton rather than mutating a published one.

## Performance evidence

Functional tests answer correctness questions; JMH answers comparative throughput under a recorded environment. Do not turn a single local `ops/s` value into a hard unit-test threshold. Compare runs only when command, JDK, fixtures, warmup, measurement mode, and metric direction match.

## Release evidence

The 0.3.0 line keeps deterministic tokenizer, detector, and sanitized-failure tests as its quality gate. Review [quality gates](../quality/quality-gates.md) for scope and [Aho-Corasick benchmarks](../quality/aho-corasick-benchmarks.md) for measured search cases.

## Source evidence

- [Korean processor tests](https://github.com/bluetape4k/bluetape4k-text/blob/0.3.0/tokenizer-korean/src/test/kotlin/io/bluetape4k/tokenizer/korean/KoreanProcessorTest.kt)
- [Japanese processor tests](https://github.com/bluetape4k/bluetape4k-text/blob/0.3.0/tokenizer-japanese/src/test/kotlin/io/bluetape4k/tokenizer/japanese/JapaneseProcessorTest.kt)
- [Safety example tests](https://github.com/bluetape4k/bluetape4k-text/blob/0.3.0/examples/tokenizer-safety-examples/src/test/kotlin/io/bluetape4k/text/examples/tokenizer/TokenizerSafetyExamplesTest.kt)
