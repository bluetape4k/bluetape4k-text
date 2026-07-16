# Learning path

This path turns the repository into a sequence of observable results. Each stage tells you what to learn, what to run, and what should be true before you continue.

## Stage 1: first result in 15 minutes

Read [Getting started](../getting-started.md) and run one Korean normalization and tokenization call.

You should be able to explain:

- why a runtime module is still required after importing a BOM;
- why structured tokens are more useful than strings for later analysis;
- where blank and oversized text should be rejected.

Continue when the example produces `안돼ㅋㅋㅋ` and `[주말, 특가, 쇼핑몰]`, and your build has no hard-coded Text module version when a BOM already manages it.

## Stage 2: choose a language route

Read [Lingua](../modules/lingua.md) and [mixed-language processing](mixed-language-processing.md), then run the [Lingua example](../examples/lingua-examples.md).

Inspect three decisions in the example:

1. the detector is built once and reused;
2. the supported language set is explicit;
3. low-accuracy mode is a separate detector choice, not a transparent optimization.

Continue when you can state how your service handles unknown, ambiguous, and mixed input. If a Unicode script check fully answers your route, you do not need a statistical detector for that decision.

## Stage 3: analyze Korean and Japanese

Read the [Korean](../modules/tokenizer-korean.md) and [Japanese](../modules/tokenizer-japanese.md) module pages. Compare their smallest examples, then inspect the [tokenizer safety example](../examples/tokenizer-safety-examples.md).

Learn the difference between:

- normalization and tokenization;
- surface text and POS-aware token data;
- application dictionary policy and packaged dictionaries;
- a valid request and a successful processor call.

Continue when tests assert stable user-visible token surfaces without coupling every assertion to internal morphological choices.

## Stage 4: search many patterns

Read [Text search](../modules/text-search.md), then run the [text search example](../examples/text-search-examples.md). Change one keyword value and observe the builder result, DSL result, replacement text, and first Flow alert.

Continue when you can choose between `parseText`, `containsMatch`, `firstMatch`, `replaceAll`, and `matchesAsFlow`; explain why the builder is not shared after publication; and identify whether your policy needs overlap, word-boundary, or normalization options.

## Stage 5: make it operable

Read [input safety](input-safety.md), [startup and memory](../operations/startup-and-memory.md), [failure contracts](../operations/failure-contracts.md), and [quality gates](../quality/quality-gates.md).

Before production, confirm:

- detectors, dictionaries, and automatons have explicit owners and lifetimes;
- warmup covers the expensive paths you actually use;
- errors contain lengths and categories, not submitted text;
- benchmark results are interpreted under their recorded environment;
- dictionary changes have persistence and rollout rules.

## Expand by problem

| Problem | Detailed path |
|---|---|
| custom dictionaries or moderation policy | [Dictionaries and blockwords](dictionaries-and-blockwords.md) |
| mixed Korean, Japanese, and Latin input | [Mixed-language processing](mixed-language-processing.md) |
| regression coverage | [Testing](testing.md) |
| exact module choice | [Capability selection](capability-selection.md) |
| performance interpretation | [Aho-Corasick benchmarks](../quality/aho-corasick-benchmarks.md) |

The module pages remain the API-oriented reference. These guides connect them into application decisions; neither is a substitute for the stable source links attached to each technical claim.
