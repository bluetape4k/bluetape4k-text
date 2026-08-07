# Dictionaries and blockwords

Dictionaries are long-lived policy data. The library provides loaders and runtime mutation APIs, but your application must define ownership, validation, persistence, and rollout.

## Load packaged resources

`DictionaryProvider` reads classpath resources as a lazy sequence, a frequency map, or a combined `CharArraySet`. Plain text and gzip-compressed resources are supported.

```kotlin
import io.bluetape4k.tokenizer.utils.DictionaryProvider

val blocked = DictionaryProvider.readWords(
    "dictionary/base.txt",
    "dictionary/product-overrides.txt",
)

if ("restricted" in blocked) {
    // apply application policy
}
```

Multiple resources are loaded through an asynchronous Flow path. Perform this work during setup, fail startup when required policy cannot load, and reuse the result.

## Korean runtime updates

```kotlin
import io.bluetape4k.tokenizer.korean.KoreanProcessor
import io.bluetape4k.tokenizer.model.Severity

KoreanProcessor.addNounsToDictionary("블루테이프4K")
KoreanProcessor.addBlockwords(listOf("금칙어"), Severity.MIDDLE)
```

Noun additions influence later morphological analysis. Blockwords are layered by severity. Define which severity the calling policy uses and test the exact masking outcome.

## Japanese runtime updates

```kotlin
import io.bluetape4k.tokenizer.japanese.JapaneseProcessor

JapaneseProcessor.addBlockwords(listOf("東京"))
JapaneseProcessor.removeBlockwords(listOf("東京"))
```

The Japanese packaged blockword dictionary loads lazily. Detection focuses on nouns and verbs and can combine adjacent tokens for a compound match.

## Policy ownership

A safe update pipeline should answer:

- who may publish a word and severity;
- how Unicode normalization and blank lines are handled;
- how duplicate or conflicting entries are resolved;
- how every application instance receives the same version;
- how a restart reconstructs runtime updates;
- how a bad release is rolled back.

The in-memory mutation API does not solve these distribution concerns. Treat it as the application point where an already validated policy snapshot is installed.

## Masking is not authorization

A masked response is a presentation result. Keep the detected policy category and word list available to the trusted decision layer, but do not expose sensitive submitted text in logs or errors. Decide whether overlapping, inflected, or compound forms require morphology, an exact automaton, or both.

## Testing

Use small fixtures for each severity and language, a compound Japanese case, a Korean runtime noun, an empty dictionary, and a restart/reload simulation. Verify that update order does not accidentally leave different instances with different policy.

Continue with [testing](testing.md), [input safety](input-safety.md), and the [quality report](../quality/quality-gates.md).

## Source evidence

- [DictionaryProvider](https://github.com/bluetape4k/bluetape4k-text/blob/0.3.0/tokenizer-core/src/main/kotlin/io/bluetape4k/tokenizer/utils/DictionaryProvider.kt)
- [KoreanProcessor](https://github.com/bluetape4k/bluetape4k-text/blob/0.3.0/tokenizer-korean/src/main/kotlin/io/bluetape4k/tokenizer/korean/KoreanProcessor.kt)
- [JapaneseProcessor](https://github.com/bluetape4k/bluetape4k-text/blob/0.3.0/tokenizer-japanese/src/main/kotlin/io/bluetape4k/tokenizer/japanese/JapaneseProcessor.kt)
