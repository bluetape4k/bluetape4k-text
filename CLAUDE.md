# CLAUDE.md — bluetape4k-text

텍스트 처리 라이브러리. 한국어·일본어 형태소 분석, 다국어 감지, Aho-Corasick 검색.

- **Group**: `io.github.bluetape4k.text` · **Version**: `0.1.0-SNAPSHOT`

## Repository Layout

```
bluetape4k-text/
├── tokenizer-core/     # Shared domain models, options, dictionary utilities, CharArraySet/Map
├── tokenizer-japanese/ # JapaneseProcessor — Kuromoji IPAdic tokenizer + blockword masking
├── tokenizer-korean/   # KoreanProcessor — full Korean NLP pipeline
├── lingua/             # LanguageDetector DSL, detectAllLanguagesOf(), UnicodeDetector
└── text-search/        # AhoCorasickAutomaton — immutable, thread-safe, generic-value
```

| Module | Artifact | Summary |
|---|---|---|
| `tokenizer-core` | `io.github.bluetape4k.text:tokenizer-core` | `TokenizeRequest/Response`, `BlockwordRequest/Response`, `Severity`, `DictionaryProvider`, `CharArraySet` |
| `tokenizer-japanese` | `io.github.bluetape4k.text:tokenizer-japanese` | `JapaneseProcessor` facade, Kuromoji IPAdic, POS extensions (`isNoun()`, `isVerb()`) |
| `tokenizer-korean` | `io.github.bluetape4k.text:tokenizer-korean` | `KoreanProcessor` facade, normalizer, chunker, POS tagger, phrase extractor, stemmer |
| `lingua` | `io.github.bluetape4k.text:lingua` | `allLanguageDetector {}`, `languageDetectorOf()`, `detectAllLanguagesOf()`, `UnicodeDetector` |
| `text-search` | `io.github.bluetape4k.text:text-search` | `AhoCorasickAutomaton<V>`, `ahoCorasick {}` DSL, `matchesAsFlow()`, `replaceAll()`, `tokenize()` |

## Build Commands

```bash
./gradlew build -x test
./gradlew build
./gradlew :tokenizer-korean:build
./gradlew :tokenizer-korean:test
./gradlew :tokenizer-korean:test --tests "io.bluetape4k.tokenizer.korean.KoreanProcessorTest"
./gradlew :text-search:benchmark
./gradlew detekt
./gradlew publishAllPublicationsToCentralPortal   # Maven Central
./gradlew publishToMavenLocal
```

## 모듈별 특이사항

### Facade 패턴

`KoreanProcessor` / `JapaneseProcessor` 는 sub-component 에 위임하는 `object` facade.
신규 기능 → 적절한 sub-component 에 먼저 추가 → facade 에 노출.

### AhoCorasickAutomaton — 빌드 후 불변

`AhoCorasickAutomaton<V>` 는 `build()` 호출 후 불변. `Builder` 만 mutable.
빌드 후 내부 상태 수정 시도 금지.

### Dictionary 로딩

`DictionaryProvider.readWords()` — 여러 파일을 `Flow.async` 로 병렬 로드.

### Flow 기반 검색

`AhoCorasickAutomaton.matchesAsFlow()` — `channelFlow + flowOn(Dispatchers.Default)` 사용.

## KDoc 예시 (이 레포 기준)

`KoreanProcessor.kt` + `AhoCorasickAutomaton.kt` 참고.
