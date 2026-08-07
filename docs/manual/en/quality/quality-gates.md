# Quality gates

The release quality evidence is a deterministic repository gate. It proves selected stable behaviors; it is not a broad statistical claim about NLP accuracy.

## Covered evidence

| Area | Stable evidence |
|---|---|
| Korean mixed-text tokenization | `KoreanTextProcessorTest` surface-token fixtures |
| Japanese mixed-text tokenization | `JapaneseProcessorTest` fixtures |
| mixed-language detection | `LanguageDetectorExtensionsTest` |
| request boundary failures | `TokenizeMessageTest`, `BlockMessageTest`, and processor facade tests |
| safety adapter behavior | runnable example tests with sanitized failure assertions |

The fixtures emphasize user-visible token surfaces. They intentionally avoid asserting every internal morphological choice, leaving room for model and dictionary improvements without hiding a user-facing regression.

## Reproduce targeted evidence

```bash
./gradlew :tokenizer-korean:test \
  --tests "io.bluetape4k.tokenizer.korean.KoreanTextProcessorTest"

./gradlew :tokenizer-japanese:test \
  --tests "io.bluetape4k.tokenizer.japanese.JapaneseProcessorTest"

./gradlew :lingua:test \
  --tests "io.bluetape4k.lingua.LanguageDetectorExtensionsTest"
```

The report was validated on macOS with JDK 21 or newer and the checked-in Gradle wrapper; Gradle 9.5.1 was observed in the recorded local output.

## What it does not prove

The report does not:

- compare against third-party NLP systems;
- publish precision, recall, or F1 over a large external corpus;
- guarantee performance or memory on another host;
- establish that a default blockword dictionary matches every product policy;
- replace application tests for supported languages and input distribution.

State these gaps when using the evidence in a release decision. A deterministic gate is valuable because it is reproducible, not because it answers every quality question.

## Extend the gate

Add a fixture when a user-visible token, route, masking result, or sanitized error becomes part of your product contract. Add scored corpus evaluation only with a versioned corpus, explicit metric, reproducible command, and interpretation threshold.

See [testing](../guides/testing.md) for application coverage and [Aho-Corasick benchmarks](aho-corasick-benchmarks.md) for performance evidence.

## Source evidence

- [Quality report](https://github.com/bluetape4k/bluetape4k-text/blob/0.3.0/docs/superpowers/research/2026-05-27-issue-86-quality-report.md)
- [Quality benchmark specification](https://github.com/bluetape4k/bluetape4k-text/blob/0.3.0/docs/superpowers/specs/2026-05-27-issue-83-text-quality-benchmark-spec.md)
