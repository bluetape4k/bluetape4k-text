# 다중 패턴 텍스트 검색 라이브러리

`text-search`는 불변 generic Aho-Corasick automaton을 제공한다. 여러 키워드를 한 번에 검색하고 각 일치에 애플리케이션 값을 연결한다. 대소문자, 겹침, 단어 경계, Unicode 정규화, 첫 일치 종료 방식을 옵션으로 정할 수 있다.

![텍스트 검색 흐름](../../assets/text-search/search-flow.png)

## 제공하는 기능

- 가변 builder와 Kotlin DSL, 빌드 뒤 불변 automaton
- `parseText`, `firstMatch`, `containsMatch`, `tokenize`, `replaceAll`
- generic 일치 값과 원문 위치
- `NONE`, `LATIN_ALPHA`, `WHITESPACE_SEPARATED` 경계
- 위치 mapping을 보존하는 NFC/NFKC 정규화
- coroutine 수집을 위한 `matchesAsFlow`

## 의존성 추가하기

```kotlin
dependencies {
    implementation("io.github.bluetape4k.text:text-search:0.3.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:<compatible-version>") // Flow 사용 시
}
```

## 가장 작은 예제

```kotlin
import io.bluetape4k.text.search.AhoCorasickAutomaton
import io.bluetape4k.text.search.SearchOptions

val automaton = AhoCorasickAutomaton.builder<String>()
    .add("password reset", "ACCOUNT_TAKEOVER")
    .add("card declined", "PAYMENT_RISK")
    .options(SearchOptions(ignoreCase = true, allowOverlaps = false))
    .build()

val matches = automaton.parseText("Password reset before card declined")
println(matches.map { it.value })
```

builder는 설정 단계에서만 사용한다. `build()`가 끝나면 불변 snapshot을 공유한다.

## 결과 연산 선택하기

| 연산 | 알맞은 상황 |
|---|---|
| `parseText` | 모든 일치와 위치가 바로 필요하다 |
| `firstMatch` | 가장 왼쪽의 가장 긴 일치 하나면 충분하다 |
| `containsMatch` | 일치 여부만 필요하다 |
| `tokenize` | 일치 구간과 일반 구간을 따로 그린다 |
| `replaceAll` | 선택된 일치를 다른 문자열로 바꾼다 |
| `matchesAsFlow` | 주변 coroutine 흐름에서 수집·취소 의미가 필요하다 |

`tokenize`는 겹치지 않는 결과를 반환한다. `allowOverlaps = false`는 다른 연산이 받기 전에 경쟁하는 일치를 정리한다.

## 경계와 정규화

`LATIN_ALPHA`는 더 긴 영단어 내부의 부분 일치를 막고, `WHITESPACE_SEPARATED`는 공백으로 둘러싸인 구문에 적합하다. Unicode 정규화는 동등한 문자를 찾게 해 주지만 비용이 크다. 0.3.0의 NFKC 벤치마크가 일반 no-match 경로보다 훨씬 느린 이유다.

## Flow 수집

```kotlin
val firstAlert = automaton.matchesAsFlow("critical login before card declined")
    .take(1)
    .toList()
```

Flow 확장은 `channelFlow`와 `Dispatchers.Default`를 사용한다. 제한 수집은 불필요한 결과 보관을 줄이지만 입력 전체를 살피는 CPU 비용까지 없애지는 않는다.

## 제약과 실패

키워드 snapshot과 옵션 정책을 먼저 확정한 뒤 공개한다. 요청마다 다시 빌드하지 않는다. 정규화가 켜져도 결과 위치는 원문을 가리키도록 mapping한다. 벤치마크 수치는 기록된 환경 안에서만 비교한다.

## 다음 학습 경로

- [텍스트 검색 실행 예제](../examples/text-search-examples.md)
- [Aho-Corasick 벤치마크](../quality/aho-corasick-benchmarks.md)
- [테스트](../guides/testing.md)

## 소스 근거

- [AhoCorasickAutomaton](https://github.com/bluetape4k/bluetape4k-text/blob/0.3.0/text-search/src/main/kotlin/io/bluetape4k/text/search/AhoCorasickAutomaton.kt)
- [SearchOptions](https://github.com/bluetape4k/bluetape4k-text/blob/0.3.0/text-search/src/main/kotlin/io/bluetape4k/text/search/SearchOptions.kt)
- [Flow 확장](https://github.com/bluetape4k/bluetape4k-text/blob/0.3.0/text-search/src/main/kotlin/io/bluetape4k/text/search/flow/AhoCorasickFlowExtensions.kt)

<!-- release-readme-diagrams:start -->
## 배포본 다이어그램 {#release-diagrams}

아래 그림은 `0.3.0` 배포본의 README 자산을 해당 배포 커밋에서 직접 불러옵니다. 이후 SNAPSHOT이 아니라 이 매뉴얼 버전의 구조와 실행 흐름을 보여 줍니다. 미리보기를 누르면 같은 배포 커밋의 SVG 원본이 열립니다.

### 처리 흐름

[![처리 흐름](https://raw.githubusercontent.com/bluetape4k/bluetape4k-text/aead213d2d25307d7d3684226943a5f95c7411f2/docs/images/readme-diagrams/text-search-architecture-03.png)](https://github.com/bluetape4k/bluetape4k-text/blob/aead213d2d25307d7d3684226943a5f95c7411f2/docs/images/readme-diagrams/text-search-architecture-03.svg)

_배포본 README: [`text-search/README.ko.md`](https://github.com/bluetape4k/bluetape4k-text/blob/aead213d2d25307d7d3684226943a5f95c7411f2/text-search/README.ko.md)_

### 텍스트 검색 클래스 구조도

[![텍스트 검색 클래스 구조도](https://raw.githubusercontent.com/bluetape4k/bluetape4k-text/aead213d2d25307d7d3684226943a5f95c7411f2/docs/images/readme-diagrams/text-search-class-01.png)](https://github.com/bluetape4k/bluetape4k-text/blob/aead213d2d25307d7d3684226943a5f95c7411f2/docs/images/readme-diagrams/text-search-class-01.svg)

_배포본 README: [`text-search/README.ko.md`](https://github.com/bluetape4k/bluetape4k-text/blob/aead213d2d25307d7d3684226943a5f95c7411f2/text-search/README.ko.md)_

### 검색 파이프라인

[![검색 파이프라인](https://raw.githubusercontent.com/bluetape4k/bluetape4k-text/aead213d2d25307d7d3684226943a5f95c7411f2/docs/images/readme-diagrams/text-search-sequence-02.png)](https://github.com/bluetape4k/bluetape4k-text/blob/aead213d2d25307d7d3684226943a5f95c7411f2/docs/images/readme-diagrams/text-search-sequence-02.svg)

_배포본 README: [`text-search/README.ko.md`](https://github.com/bluetape4k/bluetape4k-text/blob/aead213d2d25307d7d3684226943a5f95c7411f2/text-search/README.ko.md)_

<!-- release-readme-diagrams:end -->
