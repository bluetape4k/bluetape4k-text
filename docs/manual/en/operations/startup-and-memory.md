# Startup and memory

Detector models, tokenizer dictionaries, and Aho-Corasick automatons move work between startup and request time. Make those lifetimes explicit and measure them in the deployment environment.

## Resource ownership

| Resource | Setup choice | Recommended owner |
|---|---|---|
| Lingua detector | supported language set, preload or lazy models, accuracy mode | application singleton |
| Korean processor dictionaries | packaged data plus validated runtime updates | shared processor policy |
| Japanese blockword dictionary | lazy first access plus runtime updates | shared processor policy |
| custom `CharArraySet` | resource set and reload version | application dictionary service |
| Aho-Corasick automaton | keyword snapshot and search options | immutable application snapshot |

## Preload deliberately

Preloading Lingua models makes later calls more predictable but raises startup time and initial memory. Lazy loading lowers initial work but can move latency into the first request for a language. Restricting the supported set is usually more important than choosing either mode blindly.

Japanese blockword data also has a lazy first-access boundary. Exercise that call during warmup if the endpoint promises steady latency from its first accepted request.

## Build search snapshots off the request path

Construct the automaton after loading and validating a keyword version, then publish the immutable result. For updates, build a replacement snapshot and atomically swap application ownership. Do not mutate a shared builder or rebuild the same keywords per request.

## Put limits below library ceilings

The tokenizer request ceiling is `100_000` characters, but a service may need a smaller limit for its latency target. Measure representative short, median, and near-limit texts. Track character length, operation, language route, and elapsed time without recording raw text.

## Warmup checklist

1. construct the configured detector;
2. trigger every language model used by the service if lazy loading remains enabled;
3. call the Korean/Japanese operations that serve traffic;
4. load required blockword and custom dictionaries;
5. build the production-size automaton;
6. record startup duration and memory after the snapshot is ready.

Warmup should fail readiness when a required resource cannot load. It should not accept traffic and retry an immutable setup error on each request.

## Observe without leaking text

Useful metrics include resource version, supported language count, dictionary entry count, automaton keyword count, input length bucket, match count, and sanitized failure category. Never label a metric or exception with submitted text.

See [runtime boundaries](../architecture/runtime-boundaries.md), [input safety](../guides/input-safety.md), and [failure contracts](failure-contracts.md).
