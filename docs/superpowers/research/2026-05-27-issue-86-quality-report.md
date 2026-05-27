# 0.2.0 Quality Report

Date: 2026-05-27
Issues: #83, #84, #85, #86, #96

## Summary

The 0.2.0 quality gate is based on deterministic repository tests. It covers
mixed Korean/Japanese tokenization, mixed-language detection, and sanitized
request-boundary failures for tokenizer request models and public processor
facade paths.

## Evidence Matrix

| Area | Evidence | Command |
|---|---|---|
| Tokenizer quality gate definition | `docs/superpowers/specs/2026-05-27-issue-83-text-quality-benchmark-spec.md` | content review |
| Dictionary update workflow | `docs/superpowers/plans/2026-05-27-issue-85-dictionary-update-pipeline-plan.md` | content review |
| Korean mixed-text tokenizer fixtures | `KoreanTextProcessorTest` | `./gradlew :tokenizer-korean:test --tests "io.bluetape4k.tokenizer.korean.KoreanTextProcessorTest"` |
| Japanese mixed-text tokenizer fixtures | `JapaneseProcessorTest` | `./gradlew :tokenizer-japanese:test --tests "io.bluetape4k.tokenizer.japanese.JapaneseProcessorTest"` |
| Language detection fixtures | `LanguageDetectorExtensionsTest` | `./gradlew :lingua:test --tests "io.bluetape4k.lingua.LanguageDetectorExtensionsTest"` |
| Sanitized request failures | `TokenizeMessageTest`, `BlockMessageTest`, `KoreanTextProcessorTest`, `JapaneseProcessorTest` | core request tests plus Korean/Japanese processor tests in this matrix |

## Corpus Notes

The tokenizer fixtures intentionally assert stable surface tokens rather than
every internal morphological choice. That keeps the release gate focused on user
visible token coverage while allowing future model or dictionary changes to
improve internal POS details.

## Caveats

This report is a repository quality gate, not an external benchmark claim. It
does not compare against third-party NLP systems or publish statistical accuracy
over a large corpus. Future milestones can add larger corpora and scored
precision/recall metrics once the 0.2.0 deterministic gate is stable.

## Reproduction

Run the commands in the evidence matrix from the repository root on JDK 21+ with
the checked-in Gradle wrapper.

Validated environment for this report:

| Item | Value |
|---|---|
| OS | macOS local workspace |
| Date | 2026-05-27 |
| JDK | Java 21 or newer, matching the repository baseline |
| Gradle | Checked-in wrapper, Gradle 9.5.1 observed in local output |
| Commands | Targeted evidence matrix plus `./gradlew compileTestKotlin` and `./gradlew test` |
