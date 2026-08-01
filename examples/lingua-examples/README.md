# lingua-examples

This example reuses one Lingua detector to segment mixed English, Korean, and Japanese text, then routes Korean and Japanese segments to their public tokenizer facades. Segment offsets are UTF-16 indexes into the original input.

Run it with:

```bash
./gradlew :examples:lingua-examples:run
```

The test suite also verifies the segment order, source offsets, and per-language token counts.
