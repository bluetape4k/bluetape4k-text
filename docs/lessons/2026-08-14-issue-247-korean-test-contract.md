# 회고: 한국어 punctuation·dictionary lifecycle 계약 복원 (#247)

**날짜:** 2026-08-14
**이슈:** #247
**모듈:** `tokenizer-korean`

## 근본 원인

`KoreanChunker`는 문장 사이의 공백을 `KoreanPos.Space` token으로 보존한다.
기존 `PunctuationProcessor`는 인접한 세 token만 검사했기 때문에
`일반 token - Space - punctuation - Space - punctuation - Space - 일반 token`
형태의 우회 구두점 연속을 찾지 못했다. 구두점만 제거하도록 바꾸면 우회 구간의
공백이 남으므로, disabled regression test가 요구하는 결과도 만들 수 없다.

## 결정

- `normalPos` 양쪽으로 연결된 최대 `punctuation/Space` run을 하나의 후보로 본다.
- 공백이 없는 run은 기존처럼 구두점 token만 제거한다.
- 공백이 있는 run은 구두점이 둘 이상일 때만 구두점과 run 내부 공백을 함께 제거한다.
  이때 `~`처럼 문장 구분자 목록에 없는 우회 표식이 하나라도 있어야 하며,
  자연스러운 구분자인 `? !`만 있는 run은 보존한다. 따라서 `난~ 1학년`도 보존한다.
- `findPunctuation()`은 `removePunctuation()`이 실제로 삭제하는 모든 token을 같은
  `Boolean`으로 반환한다. 다중 구두점 run의 공백도 삭제 대상이면 `true`이다.
- `URL`은 기존 계약대로 우회 구두점 집합에서 제외한다. `Email`, `Hashtag`, `CashTag`는
  기존 금칙어 우회 탐지 계약을 유지한다.
- dictionary lifecycle은 base에 이미 있는 실제 `Job` 취소 후 재시도,
  concurrent reload/read snapshot 검증을 삭제하거나 약화하지 않고 module test에서 재검증한다.

## 검증 규칙

- disabled punctuation test를 활성화한 첫 실행은 공백이 포함된 연속 구두점이 그대로 남는
  assertion failure로 RED를 확인한다.
- 수정 후 punctuation test는 공백 연속 구두점, 기존 no-space 케이스, URL 보존 및 Email
  우회 규칙, 정상 문장 구두점 보존, `findPunctuation()` 제거 계획 정합성을 함께 검증한다.
- dictionary lifecycle test는 `SuspendMemoized`의 실제 `async` `Job.cancel` 경로와
  `KoreanDictionaryProvider`의 concurrent reload/read snapshot 경계를 검증한다.
- skipped test 수는 module 실행 결과에서 0이어야 한다.

## 후속 주의

단일 punctuation과 공백만 있는 표현의 의미는 자연어 문장부호와 금칙어 우회가 충돌할 수
있으므로, 새 규칙을 추가할 때는 정상 문장 구분자 보존 회귀 테스트를 함께 추가한다.
