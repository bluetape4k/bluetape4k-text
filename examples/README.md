# bluetape4k-text Examples

English | [한국어](README.ko.md)

Runnable examples for the `bluetape4k-text` modules. These modules are CI
smoke tests only; they are not published to Maven Central.

## Modules

| Module | Purpose |
|---|---|
| [`text-search-examples`](text-search-examples) | Aho-Corasick builder, DSL, replacement, and Flow first-alert search |
| [`lingua-examples`](lingua-examples) | Lingua detector reuse, mixed-language segmentation, and Korean/Japanese tokenizer routing |
| [`tokenizer-safety-examples`](tokenizer-safety-examples) | Tokenizer and blockword web-service request boundaries with 400/413/500 mapping |

## Run

```bash
./gradlew :examples:text-search-examples:run :examples:lingua-examples:run :examples:tokenizer-safety-examples:run
```
