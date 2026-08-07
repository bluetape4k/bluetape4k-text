# Processing model

Language detection, tokenization, dictionary filtering, and pattern search solve different problems. Compose only the stages your request needs.

![Text processing pipeline](../../assets/architecture/text-processing-pipeline.png)

## Stage 1: accept and constrain input

Validate text before model or dictionary work. `tokenizeRequestOf` and `blockwordRequestOf` reject blank input and enforce their respective maximum lengths. An HTTP adapter should map these cases to `400` and `413` without returning submitted content.

This stage is mandatory for an untrusted service boundary. It is distinct from morphological correctness: a valid request can still contain an unsupported or ambiguous language.

## Stage 2: identify script or language when routing needs it

`UnicodeDetector` answers a narrow, deterministic question: whether characters from a supported script occur and which characters match. A Lingua `LanguageDetector` estimates language from text and can inspect mixed input through `detectAllLanguagesOf`.

Use Unicode filtering for cheap script gates. Use statistical detection when Latin languages or ambiguous text matter. A service may combine both: script evidence can select a fast path, while Lingua handles the remaining text.

## Stage 3: run a language-specific processor

`KoreanProcessor` and `JapaneseProcessor` are facades over their tokenizer components. They return structured tokens; the Korean facade also exposes normalization, stemming, phrase extraction, sentence splitting, and detokenization. The Japanese facade exposes Kuromoji tokenization and POS-oriented filters.

Do not treat token text as the only useful result when POS, offsets, or stems are needed later. Convert to strings only at the presentation boundary.

## Stage 4: apply dictionary policy

Both processors expose blockword operations, but dictionary ownership is application policy. Runtime additions affect subsequent calls in the same process. Decide how updates are authenticated, validated, distributed, and restored after restart.

`DictionaryProvider` in `tokenizer-core` loads resource dictionaries and can combine multiple files asynchronously. Loading is setup work, not request-by-request work.

## Stage 5: search exact patterns

`AhoCorasickAutomaton<V>` associates keywords with application values. Build it once, then use `parseText`, `firstMatch`, `tokenize`, `replaceAll`, or `matchesAsFlow`. The automaton is immutable after `build()` and safe to share.

Morphological tokenization is not required before Aho-Corasick search. Add it only if your search terms depend on analyzed token forms. Otherwise raw-text search avoids unnecessary work and preserves original offsets.

## Result and failure ownership

Each stage has a separate contract:

- boundary validation owns invalid and oversized input;
- language detection owns confidence and ambiguity, not truth;
- tokenizers own analyzed token results and processor exceptions;
- dictionaries own policy matches and masking;
- search owns exact match offsets and associated values.

Keeping these boundaries visible makes testing and failure mapping much simpler. Continue with [runtime boundaries](runtime-boundaries.md), [testing](../guides/testing.md), and [failure contracts](../operations/failure-contracts.md).

## Source evidence

- [Tokenizer request models](https://github.com/bluetape4k/bluetape4k-text/tree/0.3.0/tokenizer-core/src/main/kotlin/io/bluetape4k/tokenizer/model)
- [Lingua sources](https://github.com/bluetape4k/bluetape4k-text/tree/0.3.0/lingua/src/main/kotlin/io/bluetape4k/lingua)
- [Aho-Corasick implementation](https://github.com/bluetape4k/bluetape4k-text/blob/0.3.0/text-search/src/main/kotlin/io/bluetape4k/text/search/AhoCorasickAutomaton.kt)
