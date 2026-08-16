# tokenizer-safety-examples

Runnable examples for tokenizer request safety, versioned dictionary reloads,
and the Japanese backend comparison boundary.

## Japanese backend comparison

`JapaneseBackendComparisonExamples` renders the current `Kuromoji IPADic`
runtime observation beside the official `Sudachi JVM` split-mode surface
recording using one report shape. The current side calls `JapaneseProcessor`
and records the first IPADic POS field. The candidate side is deliberately a
`RECORDED` fixture because the Sudachi system dictionary is not stored in this
repository.

The report also prints each backend's license, runtime footprint, and Gradle
dependency state. The current example uses only `bt4k.kuromoji.ipadic`; no
candidate dependency is added.

The `A/B/C` surface differences make the migration cost visible, while the
candidate POS status remains `UNMAPPED`. This is a contract and migration
example, not an accuracy or latency benchmark. Claiming runtime parity requires
separately approved Sudachi dependency and dictionary integration followed by a
same-corpus verification.

The surface fixture is grounded in the [official Sudachi split-mode
documentation](https://github.com/WorksApplications/Sudachi#the-modes-of-splitting)
and the repository's [Issue #105 evaluation](../../docs/superpowers/research/2026-08-16-issue-105-japanese-backend-evaluation.md).
The actual dictionary-backed verification is tracked in [follow-up Issue
#284](https://github.com/bluetape4k/bluetape4k-text/issues/284).

Run the example, including the comparison, with:

```bash
./gradlew :examples:tokenizer-safety-examples:run
```

## Dictionary reload flow

`DictionaryReloadExamples` loads the tiny `blockwords-v1.txt` fixture into a
`VersionedDictionary`, applies `blockwords-v2.txt` as revision 2, and runs a
blockword processor against the before/after snapshots. A loader failure is
also attempted at revision 3; the last successful snapshot remains active.

Run the module with:

```bash
./gradlew :examples:tokenizer-safety-examples:run
```

The test suite verifies input-length guards, sanitized errors, processor
routing, versioned reload output, failed-reload isolation, and the end-to-end
moderation response.

## Moderation service flow

`TextModerationService` validates the boundary, detects supported language
segments, routes Korean and Japanese segments to their facades, and uses a
single Aho-Corasick automaton for keyword/blockword matches and masking. The
response contains status, detected languages, token count, match summaries,
and sanitized errors.
