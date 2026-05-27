# Text Quality Gate And Fixture Corpus

Date: 2026-05-27
Issue: #83
Milestone: 0.2.0

## Context

`bluetape4k-text` 0.2.0 needs a small, repeatable quality gate before adding
larger tokenizer and language-detection features. The gate should assert
deterministic tokenizer fixtures and report the commands used to verify language
detection behavior. This is a release-readiness gate, not a runtime benchmark or
statistical NLP benchmark claim.

## Corpus Scope

The 0.2.0 corpus is intentionally compact and source-controlled:

| Area | Fixture shape | Evidence |
|---|---|---|
| Korean tokenizer | Mixed Korean/Japanese service text with stable Korean token surfaces | `KoreanTextProcessorTest.should keep Korean tokens stable in mixed Korean Japanese text` |
| Japanese tokenizer | Mixed Korean/Japanese service text with stable Japanese token surfaces | `JapaneseProcessorTest.tokenize - mixed Korean Japanese text preserves Japanese surfaces` |
| Language detection | English/Korean/Japanese mixed strings and emoji-only unknown input | `LanguageDetectorExtensionsTest` |
| Input safety | Oversized tokenize/blockword requests and sanitized messages | `TokenizeMessageTest`, `BlockMessageTest`, processor facade tests |

## Metrics

The release gate uses deterministic pass/fail metrics:

| Metric | Target |
|---|---|
| Korean mixed-text token coverage | Expected Korean surfaces are present for every fixture row |
| Japanese mixed-text token coverage | Expected Japanese surfaces are present for every fixture row |
| Language detection coverage | Expected language set matches for representative mixed input |
| Sanitized failure coverage | Oversized request messages include length/max values and exclude raw user text |

## Commands

Run these commands before claiming 0.2.0 quality evidence:

```bash
./gradlew :tokenizer-core:test --tests "io.bluetape4k.tokenizer.model.TokenizeMessageTest" --tests "io.bluetape4k.tokenizer.model.BlockMessageTest"
./gradlew :tokenizer-korean:test --tests "io.bluetape4k.tokenizer.korean.KoreanTextProcessorTest"
./gradlew :tokenizer-japanese:test --tests "io.bluetape4k.tokenizer.japanese.JapaneseProcessorTest" --tests "io.bluetape4k.tokenizer.japanese.block.JapaneseBlockwordProcessorTest"
./gradlew :lingua:test --tests "io.bluetape4k.lingua.LanguageDetectorExtensionsTest"
```

## Limitations

The 0.2.0 gate does not claim statistical NLP accuracy across a large external
corpus. It locks representative behavior that matters for Kotlin service
adoption: mixed Korean/Japanese text, language detector setup, and safe request
boundaries. Larger corpora and quantitative scoring can be added in later
milestones without changing the 0.2.0 release gate.
