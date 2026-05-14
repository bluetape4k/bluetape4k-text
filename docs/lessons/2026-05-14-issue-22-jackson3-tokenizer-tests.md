# Issue 22 Jackson3 Tokenizer Tests

## Context

`tokenizer-core` tests used `bluetape4k-jackson2` and Jackson2 Kotlin
extensions.

## Decision

Switch tokenizer tests to `bluetape4k-jackson3` and `tools.jackson` Kotlin
extensions. Add the minimal catalog aliases needed by the test classpath.

## Outcome

Tokenizer test serialization now compiles against Jackson3.

## Verification

- `./gradlew :tokenizer-core:testClasses`

## Future Notes

When adding a Jackson3 alias to smaller repos, also add explicit Jackson3 module
aliases for test-only direct imports instead of relying on incomplete
transitive version metadata.
