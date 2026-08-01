# text-search-examples

This example compares eager `parseText` with the bounded `AhoCorasickScanner` while a deterministic input is split across chunk boundaries. It reports global UTF-16 offsets and verifies that both paths return the same matches.

Run it with:

```bash
./gradlew :examples:text-search-examples:run
```

The test suite covers cross-boundary keywords, dense match output, and invalid chunk sizes.
