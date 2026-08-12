# Runtime boundaries

The modules share a repository but have different initialization, memory, and concurrency behavior. Treat those differences as architecture choices.

## Language detectors

Lingua detectors load language models. Preloading moves work to startup and increases the initial memory footprint; lazy loading defers work until a language is needed. In either mode, construct a detector for the supported language set and reuse it across requests.

Creating an all-language detector per call combines the broadest model set with the worst allocation pattern. Prefer the smallest supported language set and measure startup with your deployment limits.

## Korean and Japanese processors

`KoreanProcessor` and `JapaneseProcessor` are shared facade objects. The Korean API is documented as safe for concurrent use. Both facades expose a suspend `preload()` for startup/readiness; it shares the provider's single-initialization lifecycle and keeps resource I/O out of the request path. Direct synchronous access remains a compatibility fallback and can block on the first lookup.

If predictable latency matters, call the relevant facade `preload()` during application warmup and record cold/warm timing. Do not mutate runtime dictionaries casually: additions, removals, and clears change process-wide behavior for later requests.

## Dictionary loading

`DictionaryProvider.readWords` combines multiple resources with coroutine-based asynchronous loading. Call the processor facade `preload()` from a controlled setup coroutine. Resource files may be plain text or gzip-compressed, and the resulting `CharArraySet` is intended for repeated membership checks.

The loader does not define how a cluster distributes policy updates. Persist and version application-owned dictionaries outside the request path when instances must agree.

## Search automatons

`AhoCorasickAutomaton.Builder` is mutable while keywords and options are being assembled. `build()` produces an immutable automaton that can be shared. Rebuilding a large dictionary for every request discards that design advantage and repeats trie and failure-link construction.

`matchesAsFlow` uses a coroutine Flow path on `Dispatchers.Default`. It supports bounded collection such as `take(1)`, but it does not make a CPU-bound search free. Choose the regular methods for immediate in-memory results and Flow when collection and cancellation semantics fit the surrounding pipeline.

## Recommended ownership

| Resource | Recommended lifetime | Avoid |
|---|---|---|
| configured `LanguageDetector` | application singleton per language policy | build per request |
| processor facade | shared object | wrapper instances with hidden mutable policy |
| application dictionary set | versioned setup state | disk/network reload per request |
| built Aho-Corasick automaton | shared immutable snapshot | rebuild per request |
| request model | one per accepted request | storing submitted text in error logs |

## Warmup and observability

Warm only the paths you actually use, and measure the result. Useful signals include detector construction time, first-tokenization latency, dictionary size, automaton keyword count, input length, match count, and sanitized failure category. Never attach raw user text to a generic failure metric.

See [startup and memory](../operations/startup-and-memory.md) for a deployment checklist and [input safety](../guides/input-safety.md) for request-boundary rules.

## Source evidence

- [Lingua module contract](https://github.com/bluetape4k/bluetape4k-text/blob/0.3.0/lingua/README.md)
- [Japanese processor contract](https://github.com/bluetape4k/bluetape4k-text/blob/0.3.0/tokenizer-japanese/README.md)
- [Text search Flow extension](https://github.com/bluetape4k/bluetape4k-text/tree/0.3.0/text-search/src/main/kotlin/io/bluetape4k/text/search/flow)
