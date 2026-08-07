# Lingua 예제

실행 예제는 감지기 재사용, 명시적인 언어 집합, 저정확도 모드를 비교한다. 혼합 입력에는 영어·한국어·일본어 문장이 들어 있다.

## 실행하기

```bash
./gradlew :examples:lingua-examples:run
```

보고서는 세 줄을 출력한다.

```text
reuse=<영어, 한국어, 일본어 감지 집합>
subset=<영어, 한국어, 일본어 감지 집합>
lowAccuracy=ENGLISH
```

혼합 언어 결과는 Set이므로 출력 순서를 assertion하면 안 된다.

## 재사용하는 감지기

첫 감지기는 영어·한국어·일본어만 지원하고 minimum relative distance를 `0.0`으로 두며 모델을 미리 로드한다. 감지할 텍스트마다 새로 만들지 않고 구성된 인스턴스를 사용한다.

지원 정책이 정해진 서비스라면 이 형태가 기본이다. 애플리케이션 초기화 단계에서 언어 집합을 정하고 감지기를 공유한다.

## DSL로 만든 언어 집합

```kotlin
val detector = languageDetectorOf(languages) {
    withMinimumRelativeDistance(0.0)
    withPreloadedLanguageModels()
}
```

parameter 방식 감지기와 같은 제품 언어 집합을 나타내야 한다. 두 결과를 비교하면 설정이 서로 달라지는 문제를 찾기 쉽다.

## 저정확도 감지기

세 번째 감지기는 영어와 독일어만 지원하고 모델을 지연 로드하며 저정확도 모드를 켠다. 배포 fixture에서 `Hello service users`는 영어로 감지된다.

저정확도 모드는 자원과 품질의 trade-off다. 단순한 성능 flag처럼 켜지 말고 애플리케이션 corpus로 검증한다.

## 바꿔 볼 내용

1. 지원 언어 집합을 제품 정책과 같게 바꾼다.
2. 짧고 모호한 입력을 추가한다.
3. 빈 입력에서 `detectAllLanguagesOf`가 빈 집합을 반환하는지 확인한다.
4. 감지 결과와 별도로 애플리케이션 route를 검증한다.

[혼합 언어 처리](../guides/mixed-language-processing.md), [런타임 경계](../architecture/runtime-boundaries.md), [Lingua 모듈](../modules/lingua.md)을 함께 읽자.

## 소스 근거

- [실행 소스](https://github.com/bluetape4k/bluetape4k-text/blob/0.3.0/examples/lingua-examples/src/main/kotlin/io/bluetape4k/text/examples/lingua/LinguaExamples.kt)
- [예제 테스트](https://github.com/bluetape4k/bluetape4k-text/blob/0.3.0/examples/lingua-examples/src/test/kotlin/io/bluetape4k/text/examples/lingua/LinguaExamplesTest.kt)
