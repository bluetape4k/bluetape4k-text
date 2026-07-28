# 사전 및 차단어 업데이트 파이프라인 계획

날짜: 2026-05-27
이슈: #85
마일스톤: 0.2.0

## 배경

토크나이저와 차단어 데이터는 현재 classpath 리소스로 관리되며, 한국어와
일본어 processor/provider API를 통해 런타임에 확장할 수 있다. 정기적인 사전
변경이 발생하기 전에 0.2.0에서 업데이트 경로를 정의해야 한다. 런타임
reload와 버전 관리 사전 지원은 0.3.0 기능으로 남긴다.

## 현재 기준 소스

| 모듈 | 리소스 루트 | 로더 |
|---|---|---|
| tokenizer-core | generic classpath paths | `DictionaryProvider` |
| tokenizer-korean | `koreantext` | `KoreanDictionaryProvider` |
| tokenizer-japanese | `japanesetext` | `JapaneseDictionaryProvider` |

## 업데이트 워크플로

1. 담당 모듈의 리소스 루트 아래에 UTF-8 텍스트 파일로 원본 데이터를 준비한다.
2. 각 줄의 공백을 다듬고 빈 행을 제거해 정규화한다.
3. 원본 순서가 의미를 갖지 않는 경우 결정적인 순서로 행을 정렬한다.
4. 같은 사전 파일 안의 중복 항목을 제거한다.
5. 수정한 리소스 파일을 로드하는 모듈 테스트를 실행한다.
6. 변경된 사전 영역과 검증 명령을 명시한 릴리스 노트를 추가한다.

## 검증 매트릭스

| 변경 유형 | 필수 검증 |
|---|---|
| 한국어 명사/POS 사전 | `./gradlew :tokenizer-korean:test` |
| 한국어 차단어 사전 | `./gradlew :tokenizer-korean:test --tests "io.bluetape4k.tokenizer.korean.block.KoreanBlockwordProcessorTest"` |
| 일본어 차단어 사전 | `./gradlew :tokenizer-japanese:test --tests "io.bluetape4k.tokenizer.japanese.block.JapaneseBlockwordProcessorTest"` |
| 공용 사전 유틸리티 동작 | `./gradlew :tokenizer-core:test --tests "io.bluetape4k.tokenizer.utils.DictionaryProviderTest"` |

## 릴리스 노트 템플릿

```markdown
### 사전 업데이트

- 영역: 한국어 명사 사전 / 한국어 차단어 / 일본어 차단어
- 원본 파일: <paths>
- 검증: <commands>
- 호환성: public API 변경 없음 / <examples>에 대한 동작 변경
```

## 향후 작업

Issue #102가 버전 관리 사전 업데이트와 런타임 reload 지원을 담당한다. 해당
작업은 명시적인 버전 메타데이터, reload 동시성 규칙, rollback 동작을 추가해야
한다. 이 0.2.0 계획은 반복 가능한 수동 업데이트 경로만 정의한다.
