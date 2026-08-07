# 토크나이저 안전 예제

이 예제는 한국어·일본어 프로세서 앞에 작은 서비스 경계를 둔다. 제출 본문을 노출하지 않으면서 잘못된 입력, 길이 초과, 성공, 예상하지 못한 프로세서 실패를 구분한다.

## 실행하기

```bash
./gradlew :examples:tokenizer-safety-examples:run
```

프로그램은 한국어·일본어 토큰화와 금칙어 요청 네 개를 성공시킨 뒤 최대 길이를 넘는 한국어 토큰화 값을 보낸다. 성공한 줄에는 언어와 token 수 또는 masked length가 나오고, 초과 입력에는 상태 `413`, 실제 길이, 최대 길이가 나온다.

## 검증 순서

`TokenizerSafetyHandler`는 다음 순서로 처리한다.

1. 해당 연산의 최대 길이를 넘으면 `413`
2. 빈 텍스트면 `400`
3. 프로세서가 성공하면 `200`
4. 프로세서가 예외를 던지면 정제된 `500`

토큰화는 `MAX_TOKENIZE_TEXT_LENGTH`, 마스킹은 `MAX_BLOCKWORD_TEXT_LENGTH`를 사용한다. 0.3.0에서는 둘 다 `100_000`이다.

## 주입할 수 있는 프로세서 함수

handler 생성자는 한국어·일본어 함수를 받는다. 테스트에서는 실제 모델을 올리지 않고도 프로세서 실패를 만들고, 응답에 `processor error`는 있지만 제출한 sentinel은 없는지 확인할 수 있다.

이 구조는 경계 테스트를 빠르게 유지하고 adapter 정책과 토크나이저 정확성을 분리한다.

## Coroutine 주의점

예제는 동기 프로세서 호출을 `runCatching`으로 감싼다. 주입 함수가 `suspend`로 바뀐다면 `CancellationException`을 먼저 다시 던지는 명시적 `try/catch`를 사용해야 한다.

## HTTP에 적용하기

`SafetyResponse.status`를 framework 응답으로 변환하고 `body`에는 원문을 넣지 않는다. decoding 전에는 transport byte 제한을, decoding 뒤에는 라이브러리 문자 제한을 적용하고 정책상 안전한 request id만 붙인다.

## 유지할 테스트

- 빈 입력과 공백 입력
- 정확히 최대 길이와 한 글자 초과
- 각 언어와 각 연산
- 주입한 프로세서 실패
- 모든 오류와 보고서에 sentinel 원문이 없는지

[입력 안전성](../guides/input-safety.md), [실패 계약](../operations/failure-contracts.md), [Tokenizer Core](../modules/tokenizer-core.md)로 이어서 살펴보자.

## 소스 근거

- [실행 소스](https://github.com/bluetape4k/bluetape4k-text/blob/0.3.0/examples/tokenizer-safety-examples/src/main/kotlin/io/bluetape4k/text/examples/tokenizer/TokenizerSafetyExamples.kt)
- [예제 테스트](https://github.com/bluetape4k/bluetape4k-text/blob/0.3.0/examples/tokenizer-safety-examples/src/test/kotlin/io/bluetape4k/text/examples/tokenizer/TokenizerSafetyExamplesTest.kt)
