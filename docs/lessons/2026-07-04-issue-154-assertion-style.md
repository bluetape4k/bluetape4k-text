# Issue 154 Assertion Style Cleanup

## Context

The text repo still had several Java/JUnit-style or scalar assertion patterns in tests:

- `.size shouldBeEqualTo n`
- `expr shouldBeEqualTo true/false`
- `kotlin.test.assertFailsWith`
- `org.junit.jupiter.api.assertThrows`

These patterns work, but they do not follow the current bluetape4k Kotlin assertion guidance.

## Decision

Normalize tests to bluetape4k assertion intent matchers:

- `collection shouldHaveSize n`
- `predicate.shouldBeTrue()` / `predicate.shouldBeFalse()`
- `io.bluetape4k.assertions.assertFailsWith<T> { ... }`

Keep the cleanup mechanical and avoid production behavior changes.

## Outcome

The cleanup touched `text-search`, `tokenizer-core`, and `tokenizer-korean` tests plus `TrieCore` KDoc examples. Full test verification passed.

## Future Guidance

- When touching tests, scan for assertion API drift before committing.
- Prefer matcher intent over scalar projections when a bluetape4k assertion exists.
- Include JUnit `assertThrows` in future forbidden-pattern scans, not only `kotlin.test.assertFailsWith`.
