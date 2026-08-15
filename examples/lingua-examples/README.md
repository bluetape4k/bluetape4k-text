# lingua-examples

This example creates one reusable Lingua detector, segments mixed English, Korean, and Japanese text, and routes Korean and Japanese segments to their public tokenizer facades. Segment offsets are UTF-16 indexes into the original input.

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

The test suite verifies detector reuse, preload/lazy result equivalence, segment order, source offsets, and per-language token counts.
