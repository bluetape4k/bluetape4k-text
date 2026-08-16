# tokenizer-safety-examples

Runnable examples for tokenizer request safety, versioned dictionary reloads,
and the Japanese backend comparison boundary.

## Japanese backend comparison

`JapaneseBackendComparisonExamples` runs `Kuromoji IPADic` and `Sudachi JVM`
against the same approved corpus using one report shape. Both backends map the
first broad POS field into the neutral observation model. Sudachi also records
live A/B/C split-mode surfaces and C-mode POS observations.

The report also prints each backend's license, runtime footprint, dictionary
version, and Gradle dependency state. Sudachi uses the central catalog alias
`bt4k.sudachi` for the exact `com.worksap.nlp:sudachi:0.8.0` dependency. The
upstream release identifies `v0.8.*` as an intermediate series before `v1`, so
the version is intentionally pinned.

The dictionary is the official [SudachiDict `v20260428` core
release](https://github.com/WorksApplications/SudachiDict/releases/tag/v20260428).
The archive is 72,238,136 bytes with SHA-256
`40c8ffc095283f07aa06cae922e7b8147bf2919ec8830567b0b3f7a7efa3239f`; its
extracted `system_core.dic` is 217,374,303 bytes. The build verifies the
Apache-2.0 archive's `LEGAL` and `LICENSE-2.0.txt` entries. The 217 MB binary
is never committed; it is downloaded and verified under
`build/sudachi-dictionary/v20260428`.

This is not an accuracy or latency benchmark. The current conditions are JDK
25, the same three inputs (`選挙管理委員会`, `東京都へ行く`, and
`外国人参政権`), bundled Kuromoji IPADic, and SudachiDict core. For example,
Sudachi splits `選挙管理委員会` as `選挙/管理/委員/会`,
`選挙/管理/委員会`, and `選挙管理委員会` in A/B/C modes, while Kuromoji
returns `選挙/管理/委員/会`. The example records such surface/POS mismatches
as migration evidence.

The comparison is grounded in the [official Sudachi split-mode
documentation](https://github.com/WorksApplications/Sudachi#the-modes-of-splitting)
and the repository's [Issue #105 evaluation](../../docs/superpowers/research/2026-08-16-issue-105-japanese-backend-evaluation.md).
The dependency-free report shape from #116 remains intact; Issue #284 adds the
approved external dependency and dictionary-backed runtime boundary.

Run the example, including the comparison, with:

```bash
./gradlew :examples:tokenizer-safety-examples:prepareSudachiDictionary
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
