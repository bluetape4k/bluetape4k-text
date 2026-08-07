# 텍스트 검색 예제

실행 예제는 builder와 DSL을 비교하고, 위험 문구를 치환하며, Flow에서 첫 경고만 수집한다. 하나의 검색 기능에서 네 가지 결과 형태를 얻는 방법을 보여 준다.

## 실행하기

```bash
./gradlew :examples:text-search-examples:run
```

대표 출력은 다음과 같다.

```text
builder=ACCOUNT_TAKEOVER, PAYMENT_RISK
dsl=password reset, card declined
redacted=user requested [ACCOUNT_TAKEOVER] before [PAYMENT_RISK]
flow=ACCOUNT_TAKEOVER
```

Set의 출력 순서는 공개 계약이 아니다. 예제 테스트도 순서보다 포함된 값을 검증한다.

## Builder 경로

`buildRiskAutomaton()`은 `password reset → ACCOUNT_TAKEOVER`, `card declined → PAYMENT_RISK`를 등록한다. 대소문자를 무시하고 겹침을 막으며 공백으로 구분된 경계를 요구한다.

연결된 값은 치환 문자열이 아니라 애플리케이션의 위험 분류다. `parseText`가 키워드와 값을 함께 반환하므로 호출자는 어떤 근거가 일치했는지 보존한 채 정책 결정을 따로 내릴 수 있다.

## DSL 경로

DSL도 같은 문구와 경계 정책으로 automaton을 만든다. 보고서에는 일치한 keyword를 기록해 DSL이 다른 엔진이 아니라 같은 검색 모델을 구성하는 문법임을 보여 준다.

DSL로 이전하려면 양쪽 builder에서 문구 하나를 바꾸고 두 결과 집합이 같은지 먼저 확인한다.

## 치환 경로

```kotlin
val redacted = builderAutomaton.replaceAll(logLine) { match ->
    "[${match.value}]"
}
```

transform은 최종 선택된 일치를 받는다. 겹침 정책이 달라지면 치환 대상으로 선택되는 구간도 달라진다.

## 첫 Flow 경고

`collectFirstAlert`는 제한된 `Dispatchers.Default`에서 실행되고 5초 timeout 안에 `matchesAsFlow(logLine).take(1)`을 수집한다. 주변 처리도 coroutine이며 이후 일치가 필요 없을 때 알맞다. no-match 경로는 예외가 아니라 빈 목록을 반환한다.

## 바꿔 볼 내용

1. 세 번째 문구와 값을 추가한다.
2. 공백 경계를 만족하지 않는 문장을 추가한다.
3. 겹치는 문구로 `allowOverlaps` 값을 비교한다.
4. Flow 수집 취소 테스트를 추가한다.

[텍스트 검색 모듈](../modules/text-search.md), [테스트 가이드](../guides/testing.md), [벤치마크 해석](../quality/aho-corasick-benchmarks.md)으로 이어서 학습하자.

## 소스 근거

- [실행 소스](https://github.com/bluetape4k/bluetape4k-text/blob/0.3.0/examples/text-search-examples/src/main/kotlin/io/bluetape4k/text/examples/search/TextSearchExamples.kt)
- [예제 테스트](https://github.com/bluetape4k/bluetape4k-text/blob/0.3.0/examples/text-search-examples/src/test/kotlin/io/bluetape4k/text/examples/search/TextSearchExamplesTest.kt)
