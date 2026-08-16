# lingua-examples

This example creates one reusable Lingua detector, segments mixed English, Korean, and Japanese text, and routes Korean and Japanese segments to their public tokenizer facades. Segment offsets are UTF-16 indexes into the original input. The same runnable module also includes a small offline corpus quality sample.

Create the detector once at the application boundary and pass it to each pipeline call:

```kotlin
val detector = createMixedLanguageDetector(preloadModels = true)
val result = runMixedLanguagePipeline("Hello 안녕하세요 こんにちは", detector)
```

`preloadModels = true` loads all selected language models during setup, which reduces the first detection cost at the expense of startup work and memory. Set it to `false` when startup cost matters more and lazy model loading is acceptable. Either mode can be reused across inputs; do not create a detector for every call.

Run it with:

```bash
./gradlew :examples:lingua-examples:run
```

The `Offline corpus quality sample` table reports detected languages and Korean/Japanese token counts for a small checked-in fixture. It is a deterministic consumer smoke check, not a precision, recall, F1, or performance benchmark over an external corpus.

To replace the fixture with a private corpus, create a UTF-8 file using the `id<TAB>LANGUAGE[,LANGUAGE...]<TAB>text` format and pass its path:

```bash
./gradlew :examples:lingua-examples:run \
  -Dbluetape4k.offline-corpus.path=/path/to/private-corpus.tsv
```

The test suite verifies detector reuse, preload/lazy result equivalence, segment order, source offsets, per-language token counts, and the boundary between the checked-in fixture and a private corpus.
