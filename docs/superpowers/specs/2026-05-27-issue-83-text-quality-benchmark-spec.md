# 텍스트 품질 게이트와 Fixture 코퍼스

날짜: 2026-05-27
이슈: #83
마일스톤: 0.2.0

## 배경

`bluetape4k-text` 0.2.0은 더 큰 토크나이저와 언어 감지 기능을 추가하기 전에
작고 반복 가능한 품질 게이트가 필요하다. 이 게이트는 결정적인 토크나이저
fixture를 검증하고, 언어 감지 동작을 확인하는 데 사용한 명령을 보고해야 한다.
이는 릴리스 준비성 게이트이며 런타임 benchmark나 통계적 NLP benchmark 주장이
아니다.

## 코퍼스 범위

0.2.0 코퍼스는 의도적으로 작고 source-controlled 상태로 둔다.

| 영역 | Fixture 형태 | 증거 |
|---|---|---|
| 한국어 토크나이저 | 안정적인 한국어 표면 토큰을 가진 한국어/일본어 혼합 서비스 텍스트 | `KoreanTextProcessorTest.should keep Korean tokens stable in mixed Korean Japanese text` |
| 일본어 토크나이저 | 안정적인 일본어 표면 토큰을 가진 한국어/일본어 혼합 서비스 텍스트 | `JapaneseProcessorTest.tokenize - mixed Korean Japanese text preserves Japanese surfaces` |
| 언어 감지 | 영어/한국어/일본어 혼합 문자열과 emoji-only unknown 입력 | `LanguageDetectorExtensionsTest` |
| 입력 안전성 | 과도하게 큰 tokenize/blockword 요청과 정제된 메시지 | `TokenizeMessageTest`, `BlockMessageTest`, processor facade 테스트 |

## 지표

릴리스 게이트는 결정적인 pass/fail 지표를 사용한다.

| 지표 | 목표 |
|---|---|
| 한국어 혼합 텍스트 토큰 coverage | 모든 fixture 행에 예상 한국어 표면 토큰이 존재 |
| 일본어 혼합 텍스트 토큰 coverage | 모든 fixture 행에 예상 일본어 표면 토큰이 존재 |
| 언어 감지 coverage | 대표 혼합 입력에 대해 예상 언어 집합이 일치 |
| 정제된 실패 coverage | 과도하게 큰 요청 메시지는 length/max 값을 포함하고 원본 사용자 텍스트는 제외 |

## 명령

0.2.0 품질 증거를 주장하기 전에 다음 명령을 실행한다.

```bash
./gradlew :tokenizer-core:test --tests "io.bluetape4k.tokenizer.model.TokenizeMessageTest" --tests "io.bluetape4k.tokenizer.model.BlockMessageTest"
./gradlew :tokenizer-korean:test --tests "io.bluetape4k.tokenizer.korean.KoreanTextProcessorTest"
./gradlew :tokenizer-japanese:test --tests "io.bluetape4k.tokenizer.japanese.JapaneseProcessorTest" --tests "io.bluetape4k.tokenizer.japanese.block.JapaneseBlockwordProcessorTest"
./gradlew :lingua:test --tests "io.bluetape4k.lingua.LanguageDetectorExtensionsTest"
```

## 제한 사항

0.2.0 게이트는 대규모 외부 코퍼스에 대한 통계적 NLP 정확도를 주장하지 않는다.
Kotlin 서비스 도입에 중요한 대표 동작, 즉 한국어/일본어 혼합 텍스트, 언어
감지기 설정, 안전한 요청 경계를 고정한다. 더 큰 코퍼스와 정량 점수화는 0.2.0
릴리스 게이트를 바꾸지 않고 이후 milestone에서 추가할 수 있다.
