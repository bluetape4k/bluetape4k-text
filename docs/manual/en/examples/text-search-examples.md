# Text search example

The runnable example compares the builder and DSL, replaces risk phrases, and collects only the first Flow alert. It demonstrates four result shapes from one search capability.

## Run it

```bash
./gradlew :examples:text-search-examples:run
```

Representative output:

```text
builder=ACCOUNT_TAKEOVER, PAYMENT_RISK
dsl=password reset, card declined
redacted=user requested [ACCOUNT_TAKEOVER] before [PAYMENT_RISK]
flow=ACCOUNT_TAKEOVER
```

Set iteration and formatting order should not be treated as a public contract; the example tests assert the contained values.

## Builder path

`buildRiskAutomaton()` registers `password reset → ACCOUNT_TAKEOVER` and `card declined → PAYMENT_RISK`, enables case-insensitive matching, disables overlaps, and requires whitespace-separated boundaries.

The values are application categories, not replacement text. `parseText` returns both keywords and values, allowing the caller to preserve evidence and make a separate policy decision.

## DSL path

The DSL builds an automaton with the same phrases and boundary policy. Its report records matched keywords to show that the DSL is configuration syntax over the same search model, not a different engine.

Change one phrase in both builders and verify both result sets before adopting a DSL migration.

## Replacement path

```kotlin
val redacted = builderAutomaton.replaceAll(logLine) { match ->
    "[${match.value}]"
}
```

The transform receives the accepted match. Replacement uses the resolved match set, so overlap policy affects what is redacted.

## First Flow alert

`collectFirstAlert` runs on a limited `Dispatchers.Default` view, applies a five-second timeout, and collects `matchesAsFlow(logLine).take(1)`. This is useful when the surrounding pipeline is already coroutine-based and later matches are irrelevant.

The example also contains a bounded no-match path. A no-match result is an empty list, not an exception.

## Modify it safely

1. Add a third phrase and value.
2. Add a sentence where the phrase is embedded without the configured whitespace boundary.
3. Compare `allowOverlaps = true` and `false` with overlapping phrases.
4. Add a cancellation test around Flow collection.

Continue with the [Text search module](../modules/text-search.md), [testing guide](../guides/testing.md), and [benchmark interpretation](../quality/aho-corasick-benchmarks.md).

## Source evidence

- [Runnable source](https://github.com/bluetape4k/bluetape4k-text/blob/0.3.0/examples/text-search-examples/src/main/kotlin/io/bluetape4k/text/examples/search/TextSearchExamples.kt)
- [Example tests](https://github.com/bluetape4k/bluetape4k-text/blob/0.3.0/examples/text-search-examples/src/test/kotlin/io/bluetape4k/text/examples/search/TextSearchExamplesTest.kt)
