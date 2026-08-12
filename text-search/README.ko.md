한국어 | [English](./README.md)

# text-search

Kotlin/JVM용 Aho-Corasick 다중 키워드 검색 라이브러리입니다. N개의 키워드를 O(n+m+z) 시간 복잡도로 단일 패스에 동시 검색하며, 유니코드 정규화, 단어 경계 설정, 대소문자 무시, Kotlin 코루틴 Flow API를 완벽하게 지원합니다.

## 아키텍처

![text search Class Structure diagram](../docs/images/readme-diagrams/text-search-class-01.png)

### 검색 파이프라인

![Search Pipeline diagram](../docs/images/readme-diagrams/text-search-sequence-02.png)

### 처리 흐름

![Processing Flow diagram](../docs/images/readme-diagrams/text-search-architecture-03.png)

## 주요 기능

| 기능 | 설명 |
|------|------|
| **O(n+m+z) 검색** | N개의 키워드를 단일 패스에 동시 검색 |
| **제네릭 값** | 각 키워드에 임의 타입 `V` 연관 |
| **대소문자 무시** | `SearchOptions(ignoreCase = true)` |
| **겹침 제어** | `allowOverlaps = false` — 더 긴 키워드 우선 |
| **단어 경계** | `LATIN_ALPHA` 또는 `WHITESPACE_SEPARATED` |
| **유니코드 NFC/NFKC** | 매칭 전 정규화 (오프셋 매핑 자동 처리) |
| **첫 번째 매치** | `firstMatch()` — leftmost-longest (R5 규칙) |
| **토크나이즈** | `tokenize()` — Match/Fragment 토큰으로 분해 |
| **치환** | `replaceAll(text) { match → 치환값 }` |
| **Flow API** | `matchesAsFlow(text)` — Kotlin 코루틴 Flow |
| **DSL 빌더** | `ahoCorasick { }` 최상위 함수 |
| **스레드 안전** | 빌드 후 불변(immutable) |

## 사용 방법

### 기본 빌더 API

```kotlin
val automaton = AhoCorasickAutomaton.builder<String>()
    .add("apple", "APPLE")
    .add("banana", "BANANA")
    .add("cherry", "CHERRY")
    .options(SearchOptions(ignoreCase = true))
    .build()

val matches = automaton.parseText("I like Apple and BANANA.")
// [AhoCorasickMatch(start=7, end=11, keyword="apple", value="APPLE"),
//  AhoCorasickMatch(start=17, end=22, keyword="banana", value="BANANA")]
```

### DSL 빌더

```kotlin
val automaton = ahoCorasick<String> {
    ignoreCase = true
    allowOverlaps = false
    keyword("apple", "APPLE")
    keyword("banana", "BANANA")
    keywords("cherry" to "CHERRY", "date" to "DATE")
}
```

### 단순 키워드 집합

```kotlin
val automaton = ahoCorasickOf("apple", "banana", "cherry",
    options = SearchOptions(ignoreCase = true))
val found = automaton.containsMatch("I love apple pie")  // true
```

### 금칙어 마스킹

```kotlin
val profanity = listOf("바보", "멍청이", "못난이")
val automaton = AhoCorasickAutomaton.builder<String>()
    .apply { profanity.forEach { add(it, "[검열됨]") } }
    .options(SearchOptions(normalization = NormalizationForm.NFC))
    .build()

val masked = automaton.replaceAll("너는 바보야! 멍청이처럼 굴지 마.") { match -> match.value }
// "너는 [검열됨]야! [검열됨]처럼 굴지 마."
```

### 코드 구문 하이라이트 (토크나이즈)

```kotlin
val keywords = listOf("fun", "val", "var", "class", "return", "if", "for")
val automaton = ahoCorasick<String> {
    wordBoundary = WordBoundary.LATIN_ALPHA
    keywords.forEach { keyword(it, it) }
}

val tokens = automaton.tokenize("fun greet() { val name = \"world\"; return name }")
val html = buildString {
    tokens.forEach { token ->
        when (token) {
            is SearchToken.Match    -> append("<b>${token.text}</b>")
            is SearchToken.Fragment -> append(token.text)
        }
    }
}
// "<b>fun</b> greet() { <b>val</b> name = \"world\"; <b>return</b> name }"
```

### Flow API — 첫 번째 알람

```kotlin
val automaton = AhoCorasickAutomaton.builder<String>()
    .add("ERROR", "ALERT_ERROR")
    .add("WARN", "ALERT_WARN")
    .add("FATAL", "ALERT_FATAL")
    .build()

val logLine = "2026-04-26 INFO Starting... WARN disk low ERROR disk full"

val firstAlert = automaton.matchesAsFlow(logLine)
    .take(1)
    .toList()
// [AhoCorasickMatch(keyword="WARN", value="ALERT_WARN")]
```

### 한국어 유니코드 정규화

```kotlin
val automaton = AhoCorasickAutomaton.builder<String>()
    .add("나라", "COUNTRY")
    .options(SearchOptions(normalization = NormalizationForm.NFC))
    .build()

val result = automaton.parseText("아름다운 나라")
// 자모 분리 형태의 입력도 "나라" 매치
```

## API 참조

### `AhoCorasickAutomaton<V>`

| 메서드 | 설명 |
|--------|------|
| `parseText(text)` | 텍스트에서 모든 매치 반환 |
| `firstMatch(text)` | leftmost-longest 매치 1건 반환 (R5 규칙) |
| `containsMatch(text)` | 매치 존재 여부 반환 (첫 매치 즉시 반환) |
| `tokenize(text)` | `Match`/`Fragment` 토큰으로 분해; `allowOverlaps` 설정과 무관하게 항상 비겹침 시퀀스 반환 |
| `replaceAll(text) { }` | 변환 람다로 모든 매치 치환 |

### `SearchOptions`

| 필드 | 기본값 | 설명 |
|------|--------|------|
| `ignoreCase` | `false` | 대소문자 무시 |
| `allowOverlaps` | `true` | 겹치는 매치 허용 |
| `wordBoundary` | `NONE` | 단어 경계 탐지 방식 |
| `normalization` | `NONE` | 유니코드 정규화 형식 |
| `stopOnFirstMatch` | `false` | 첫 매치 후 중단 (`matchesAsFlow`에서는 무시됨) |

### `WordBoundary`

| 값 | 설명 |
|----|------|
| `NONE` | 부분 문자열 매치 허용 (경계 없음) |
| `LATIN_ALPHA` | 알파벳 경계 (`Character.isAlphabetic` 기준) |
| `WHITESPACE_SEPARATED` | 공백으로 구분된 토큰 경계 |

### Flow 확장

```kotlin
// matchesAsFlow는 channelFlow + flowOn(Dispatchers.Default)으로 실행됨
fun <V> AhoCorasickAutomaton<V>.matchesAsFlow(text: CharSequence): Flow<AhoCorasickMatch<V>>
```

## 실행 예제

[`../examples/text-search-examples`](../examples/text-search-examples)에 builder API,
`ahoCorasick` DSL, replacement, `matchesAsFlow(...).take(1)` 조기 수집을 다루는
console 예제가 있습니다.

## 벤치마크

처리량은 저장소의 kotlinx-benchmark Gradle 태스크를 통해 JMH로 측정합니다.
`ops/s`는 높을수록 좋습니다. 0.2.1 기준선은 큰 사전, dense match, no-match
입력, Unicode 정규화, Flow 수집 비용을 함께 다룹니다.

실행 조건:

- 명령: `./gradlew :text-search:benchmark`
- 호스트: Apple M4 Pro, 메모리 48 GiB
- benchmark JSON 기준 JVM: GraalVM JDK 21.0.11
- raw 결과: [`docs/benchmark/2026-06-04-issue-97-ahocorasick-baselines.json`](../docs/benchmark/2026-06-04-issue-97-ahocorasick-baselines.json)

| 벤치마크 | Ops/s | 비고 |
|----------|-------|------|
| `parseTextNoMatch` | 12,209.23 | 5,000-keyword automaton, 매치 없음 |
| `parseTextDenseMatches` | 3,566.90 | 겹치는 dense match |
| `parseTextLargeDictionary` | 3,116.99 | 5,000개 키워드, 2,000개 매치 토큰 |
| `matchesAsFlowLargeDictionaryCollect` | 712.62 | 큰 사전 입력의 Flow 전체 수집 |
| `naiveContainsSmallDictionary` | 248.39 | 1,000-keyword 순차 `String.contains` 기준선 |
| `parseTextNfkcNormalization` | 3.68 | NFKC + ignore-case 정규화 경로 |

> 이 수치는 로컬 비교용 snapshot이며 production ranking 이 아닙니다. 이후 비교는
> 같은 명령과 같은 metric direction 을 기준으로 수행하세요.

#### VersionedDictionary mutation benchmark (Issue #239)

dictionary benchmark는 Korean production provider를 직접 호출해 변경 entry
copy-on-write 경로와 full replacement 경로를 비교합니다. 각 invocation은
Noun 규모 사전에 단어를 추가한 뒤 제거하는 pair를 수행해 동일 cardinality를
복원하고, 두 mutation을 기준으로 처리량을 정규화합니다.

실행 조건:

- 명령: `./gradlew :text-search:dictionaryBenchmark`
- 호스트/JVM: Apple M4 Pro, GraalVM JDK 25.0.4, JMH thread 1개, fork 1개
- raw 결과: [`docs/benchmark/2026-08-11-issue-239-versioned-dictionary-baselines.json`](../docs/benchmark/2026-08-11-issue-239-versioned-dictionary-baselines.json)

| 벤치마크 | Ops/s | 비고 |
|----------|-------|------|
| `addRemoveWithCopyOnWrite` | 63.85 ± 29.30 | production 변경 entry COW 경로 |
| `addRemoveWithFullReplacement` | 43.00 ± 36.52 | production full replacement 경로 |

두 결과의 신뢰구간이 겹치므로 이 실행만으로 통계적으로 유의한 처리량
개선을 확정하지 않습니다. raw JSON에는 `gc.alloc.rate.norm` secondary metric이
없으므로 이 실행에서 allocation/heap-retention 개선도 주장하지 않습니다.
retention 경계는 결정적 bounded-history 테스트로 증명합니다.

#### Dictionary preload timing diagnostic (Issue #262)

preload timing diagnostic은 별도의 `JavaExec` JVM에서 실행합니다. 각 provider의
첫 번째 `preload()` 호출을 `coldMs`, 같은 프로세스의 두 번째 호출을 `warmMs`로
기록합니다. 이 task는 재현 가능한 관측용이며 JMH 처리량 벤치마크나 성능 승인
임계값이 아닙니다.

실행 명령:

```bash
./gradlew :text-search:preloadTimingDiagnostic --no-build-cache --rerun-tasks --console=plain
```

로컬 raw 결과와 실행 환경은
[`docs/benchmark/2026-08-12-issue-262-preload-timing.json`](../docs/benchmark/2026-08-12-issue-262-preload-timing.json)에
보존합니다. 이후 결과는 명령, JVM, host 등급, metric 방향이 호환될 때만
비교합니다.

로컬 벤치마크 실행:

```bash
./gradlew :text-search:benchmark
./gradlew :text-search:dictionaryBenchmark
```

## 의존성

| 의존성 | 목적 |
|---|---|
| `bluetape4k-core` | 핵심 유틸리티 |
| `kotlinx-coroutines-core` | Flow API 지원 (선택사항, `compileOnly`) |

```kotlin
// build.gradle.kts
implementation("io.github.bluetape4k.text:text-search:<current release or snapshot>")

// 선택사항: 코루틴 Flow 지원
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
```
