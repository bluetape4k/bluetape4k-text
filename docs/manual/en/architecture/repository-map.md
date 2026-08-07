# Repository map

The repository separates reusable runtime libraries from runnable examples. Only the six artifacts in the upper part of this map are published; the three examples are CI smoke applications and learning material.

![Capability and module map](../../assets/architecture/capability-map.png)

## Published artifacts

| Artifact | Responsibility | Depends on another Text artifact? |
|---|---|---|
| `bluetape4k-text-bom` | aligns Text artifact versions | constrains all five runtime modules |
| `tokenizer-core` | request/response models, options, severity, dictionaries, compact character collections | no |
| `tokenizer-korean` | Korean normalization, tokenization, stemming, phrase extraction, sentence splitting, blockwords | `tokenizer-core` |
| `tokenizer-japanese` | Kuromoji IPAdic tokenization, POS filtering, blockwords | `tokenizer-core` |
| `lingua` | Lingua detector factories, mixed-language extension, Unicode script filters | no Text runtime dependency |
| `text-search` | immutable Aho-Corasick automaton, DSL, replacement, Flow matching | no required Text runtime dependency |

The BOM is metadata. It belongs in dependency management, not in application code. `tokenizer-core` is a foundation for processor implementations and safe request boundaries; most applications should depend on a Korean or Japanese processor rather than core alone.

## Runnable examples

| Gradle project | What it proves |
|---|---|
| `:examples:text-search-examples` | builder and DSL produce equivalent risk matches; replacement and first Flow alert work |
| `:examples:lingua-examples` | a detector can be reused, restricted to a language subset, and configured for low accuracy |
| `:examples:tokenizer-safety-examples` | blank, oversized, and processor failures map to sanitized service responses |

Run all three:

```bash
./gradlew :examples:text-search-examples:run \
  :examples:lingua-examples:run \
  :examples:tokenizer-safety-examples:run
```

The examples are not published to Maven Central. Copy the demonstrated boundary or composition into your application; do not add an example project as a dependency.

## Common compositions

### Route by language

Use `lingua` to identify likely languages, then send supported text to `tokenizer-korean` or `tokenizer-japanese`. Keep an explicit fallback for unknown or ambiguous input. The [mixed-language guide](../guides/mixed-language-processing.md) explains why detection should guide routing rather than act as unquestionable truth.

### Tokenize and filter

Use a language processor for morphology and its blockword facilities for dictionary-backed filtering. Use `tokenizer-core` request models at the external boundary. The [dictionary guide](../guides/dictionaries-and-blockwords.md) covers runtime updates and ownership.

### Search without morphology

Use `text-search` when you already know the exact patterns and need to find many of them in one pass. It can search raw text independently of the tokenizers. Apply explicit normalization and word-boundary options when the input contract requires them.

## Source evidence

- [Release project registration](https://github.com/bluetape4k/bluetape4k-text/blob/0.3.0/settings.gradle.kts)
- [Release module overview](https://github.com/bluetape4k/bluetape4k-text/blob/0.3.0/README.md)
- [Examples overview](https://github.com/bluetape4k/bluetape4k-text/blob/0.3.0/examples/README.md)

<!-- release-readme-diagrams:start -->
## Release diagrams {#release-diagrams}

These diagrams are loaded directly from README assets published with the `0.3.0` release and pinned to its immutable commit. They describe this manual's released structure and runtime flows, not later Snapshot changes. Select a preview to open the SVG at the same release commit.

### text Architecture diagram

[![text Architecture diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-text/aead213d2d25307d7d3684226943a5f95c7411f2/docs/images/readme-diagrams/bluetape4k-text-architecture-01.png)](https://github.com/bluetape4k/bluetape4k-text/blob/aead213d2d25307d7d3684226943a5f95c7411f2/docs/images/readme-diagrams/bluetape4k-text-architecture-01.svg)

_Release README: [`README.md`](https://github.com/bluetape4k/bluetape4k-text/blob/aead213d2d25307d7d3684226943a5f95c7411f2/README.md)_

### Bluetape4k Text module composition chart

[![Bluetape4k Text module composition chart](https://raw.githubusercontent.com/bluetape4k/bluetape4k-text/aead213d2d25307d7d3684226943a5f95c7411f2/docs/images/readme-diagrams/root-readme-module-chart-01.png)](https://github.com/bluetape4k/bluetape4k-text/blob/aead213d2d25307d7d3684226943a5f95c7411f2/docs/images/readme-diagrams/root-readme-module-chart-01.svg)

_Release README: [`README.md`](https://github.com/bluetape4k/bluetape4k-text/blob/aead213d2d25307d7d3684226943a5f95c7411f2/README.md)_

### Bluetape4k Text overview diagram

[![Bluetape4k Text overview diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-text/aead213d2d25307d7d3684226943a5f95c7411f2/docs/images/readme-diagrams/root-readme-overview-01.png)](https://github.com/bluetape4k/bluetape4k-text/blob/aead213d2d25307d7d3684226943a5f95c7411f2/docs/images/readme-diagrams/root-readme-overview-01.svg)

_Release README: [`README.md`](https://github.com/bluetape4k/bluetape4k-text/blob/aead213d2d25307d7d3684226943a5f95c7411f2/README.md)_

<!-- release-readme-diagrams:end -->
