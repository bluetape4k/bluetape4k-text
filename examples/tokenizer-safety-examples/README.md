# tokenizer-safety-examples

Runnable examples for request safety and versioned dictionary reloads in the
tokenizer modules.

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
