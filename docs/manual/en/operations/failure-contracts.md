# Failure contracts

A text service should distinguish caller-correctable input, ambiguous routing, setup failure, and unexpected processor failure. The library supplies some guards; the application owns transport mapping and sanitization.

## Contract matrix

| Failure | Owner | Suggested outcome | Retry? |
|---|---|---|---|
| blank text | request adapter and core model | 400 | after caller correction |
| text over operation limit | request adapter and core model | 413 | only with smaller input |
| unsupported or ambiguous language | routing policy | documented 4xx or fallback | depends on caller hint |
| required dictionary/model cannot load | application startup | fail readiness/startup | after configuration repair |
| unexpected tokenizer/detector/search exception | service boundary | sanitized 500 | according to service policy |
| coroutine cancellation | coroutine owner | propagate cancellation | no automatic conversion to 500 |

## Preserve sanitization

Error messages may contain status, operation, language route, actual length, maximum length, and a policy-safe request id. They must not contain the submitted text, a matched sensitive word, or a full replacement context.

The public request models include the actual and maximum lengths in oversized-input messages. A transport adapter may use those values while still omitting raw input.

## Route ambiguity is not a processor exception

An empty or multi-language detection result should follow an explicit product route. Do not call every processor until one stops throwing. Detection uncertainty is normal input state and should be observable separately from internal failure.

## Setup failures should be sticky

Missing required dictionaries, invalid policy snapshots, and failed automaton construction should prevent a new snapshot from becoming active. Keep the last known-good immutable snapshot when product policy allows it, or fail readiness. Repeating setup on every request amplifies an outage.

## Cancellation must escape

`matchesAsFlow` participates in coroutine cancellation. Application code that catches broad exceptions around suspend calls must rethrow `CancellationException`. Otherwise shutdowns and client cancellations become false internal errors and keep unnecessary work alive.

## Verification checklist

- boundary tests cover blank, limit, and over-limit values;
- error output omits a unique raw-text sentinel;
- unknown and ambiguous routes are deterministic;
- setup publishes only a fully validated snapshot;
- processor errors are sanitized and classified;
- cancellation reaches the caller;
- metrics use categories and lengths, not text.

Continue with [input safety](../guides/input-safety.md), the [safety example](../examples/tokenizer-safety-examples.md), and [testing](../guides/testing.md).

## Source evidence

- [Tokenize request contract](https://github.com/bluetape4k/bluetape4k-text/blob/0.3.0/tokenizer-core/src/main/kotlin/io/bluetape4k/tokenizer/model/TokenizeRequest.kt)
- [Blockword request contract](https://github.com/bluetape4k/bluetape4k-text/blob/0.3.0/tokenizer-core/src/main/kotlin/io/bluetape4k/tokenizer/model/BlockwordRequest.kt)
- [Safety handler](https://github.com/bluetape4k/bluetape4k-text/blob/0.3.0/examples/tokenizer-safety-examples/src/main/kotlin/io/bluetape4k/text/examples/tokenizer/TokenizerSafetyExamples.kt)
