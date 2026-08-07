# 실패 계약

텍스트 서비스는 호출자가 고칠 수 있는 입력, 모호한 라우팅, 초기화 실패, 예상하지 못한 프로세서 실패를 구분해야 한다. 라이브러리는 일부 방어선을 제공하고, transport 변환과 정보 정제는 애플리케이션이 맡는다.

## 실패 분류표

| 실패 | 소유자 | 권장 결과 | 재시도 |
|---|---|---|---|
| 빈 텍스트 | 요청 adapter와 core 모델 | 400 | 호출자가 고친 뒤 |
| 연산 최대 길이 초과 | 요청 adapter와 core 모델 | 413 | 입력을 줄인 뒤 |
| 지원하지 않거나 모호한 언어 | 라우팅 정책 | 문서화한 4xx 또는 fallback | 언어 hint 정책에 따라 |
| 필수 사전·모델 로딩 실패 | 애플리케이션 시작 과정 | readiness·시작 실패 | 설정을 고친 뒤 |
| 예상하지 못한 처리 예외 | 서비스 경계 | 정제된 500 | 서비스 정책에 따라 |
| coroutine 취소 | coroutine 소유자 | 취소 전파 | 500으로 바꾸지 않음 |

## 정보 정제 유지하기

오류에는 상태, 연산, 언어 route, 실제 길이, 최대 길이, 정책상 안전한 request id를 넣을 수 있다. 제출 본문, 일치한 민감 단어, 치환 전후 전체 문맥은 넣지 않는다.

공개 요청 모델의 길이 초과 메시지에는 실제 길이와 최대 길이가 포함된다. transport adapter는 이 값을 사용하면서 원문을 제외할 수 있다.

## 모호한 언어는 프로세서 오류가 아니다

빈 감지 결과나 여러 언어 결과는 제품의 명시적인 route를 따른다. 하나가 성공할 때까지 모든 프로세서를 호출하지 않는다. 언어 감지의 불확실성은 정상적인 입력 상태이며 내부 오류와 다른 지표로 관측해야 한다.

## 초기화 실패는 요청마다 반복하지 않기

필수 사전 누락, 잘못된 정책 snapshot, automaton 생성 실패가 발생하면 새 snapshot을 공개하지 않는다. 정책이 허용한다면 이전 정상 snapshot을 유지하고, 그렇지 않다면 readiness를 실패시킨다. 요청마다 초기화를 반복하면 장애가 커진다.

## 취소 전파하기

`matchesAsFlow`는 coroutine 취소에 참여한다. suspend 호출을 넓게 catch하는 애플리케이션은 `CancellationException`을 먼저 다시 던져야 한다. 그렇지 않으면 종료와 client 취소가 내부 오류로 바뀌고 불필요한 작업이 계속된다.

## 검증 체크리스트

- 빈 입력, 최대 길이, 초과 입력 테스트가 있다.
- 오류 출력에 고유 원문 sentinel이 없다.
- unknown·ambiguous route가 결정적이다.
- 초기화가 완전히 검증된 snapshot만 공개한다.
- 프로세서 오류를 정제하고 분류한다.
- 취소가 호출자에게 전달된다.
- metric은 분류와 길이만 사용한다.

[입력 안전성](../guides/input-safety.md), [안전 예제](../examples/tokenizer-safety-examples.md), [테스트](../guides/testing.md)로 이어서 확인하자.

## 소스 근거

- [Tokenize 요청 계약](https://github.com/bluetape4k/bluetape4k-text/blob/0.3.0/tokenizer-core/src/main/kotlin/io/bluetape4k/tokenizer/model/TokenizeRequest.kt)
- [Blockword 요청 계약](https://github.com/bluetape4k/bluetape4k-text/blob/0.3.0/tokenizer-core/src/main/kotlin/io/bluetape4k/tokenizer/model/BlockwordRequest.kt)
- [Safety handler](https://github.com/bluetape4k/bluetape4k-text/blob/0.3.0/examples/tokenizer-safety-examples/src/main/kotlin/io/bluetape4k/text/examples/tokenizer/TokenizerSafetyExamples.kt)
