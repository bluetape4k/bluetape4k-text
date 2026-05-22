# Issue 67 Matches Flow Streaming

## Context

`matchesAsFlow()` documented cooperative streaming but delegated to
`parseText(text)` before emitting, so `take(N)` could not avoid full match
materialization.

## Decision

Use raw trie traversal for the default Flow path and send matches through the
channel as they are found. Keep eager `parseText` emission for options that need
whole-result post-processing, such as overlap removal or word-boundary filters.

## Outcome

Default `matchesAsFlow()` no longer depends on `parseText` materialization and
ignores `stopOnFirstMatch` as documented. A regression test verifies that
`parseText` returns one match while Flow still emits all matches for the same
automaton.

## Verification

`./gradlew :text-search:test --tests 'io.bluetape4k.text.search.flow.AhoCorasickFlowTest' --no-daemon --no-configuration-cache --no-build-cache`

## Future Notes

Do not remove the eager fallback unless overlap and word-boundary filtering can
be applied incrementally without changing match ordering.
