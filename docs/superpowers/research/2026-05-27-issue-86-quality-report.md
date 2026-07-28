# 0.2.0 품질 보고서

날짜: 2026-05-27
이슈: #83, #84, #85, #86, #96

## 요약

0.2.0 품질 게이트는 결정적인 저장소 테스트를 기준으로 한다. 이 게이트는
한국어/일본어 혼합 텍스트 토큰화, 혼합 언어 감지, 토크나이저 요청 모델과 public
processor facade 경로의 정제된 요청 경계 실패를 다룬다.

## 증거 매트릭스

| 영역 | 증거 | 명령 |
|---|---|---|
| 토크나이저 품질 게이트 정의 | `docs/superpowers/specs/2026-05-27-issue-83-text-quality-benchmark-spec.md` | 내용 검토 |
| 사전 업데이트 워크플로 | `docs/superpowers/plans/2026-05-27-issue-85-dictionary-update-pipeline-plan.md` | 내용 검토 |
| 한국어 혼합 텍스트 토크나이저 fixture | `KoreanTextProcessorTest` | `./gradlew :tokenizer-korean:test --tests "io.bluetape4k.tokenizer.korean.KoreanTextProcessorTest"` |
| 일본어 혼합 텍스트 토크나이저 fixture | `JapaneseProcessorTest` | `./gradlew :tokenizer-japanese:test --tests "io.bluetape4k.tokenizer.japanese.JapaneseProcessorTest"` |
| 언어 감지 fixture | `LanguageDetectorExtensionsTest` | `./gradlew :lingua:test --tests "io.bluetape4k.lingua.LanguageDetectorExtensionsTest"` |
| 정제된 요청 실패 | `TokenizeMessageTest`, `BlockMessageTest`, `KoreanTextProcessorTest`, `JapaneseProcessorTest` | 이 매트릭스의 core 요청 테스트와 한국어/일본어 processor 테스트 |

## 코퍼스 메모

토크나이저 fixture는 모든 내부 형태소 선택이 아니라 안정적인 표면 토큰을
의도적으로 검증한다. 이렇게 하면 릴리스 게이트는 사용자에게 보이는 토큰
coverage에 집중하면서도, 향후 모델이나 사전 변경이 내부 POS 세부 정보를 개선할
수 있게 한다.

## 주의 사항

이 보고서는 저장소 품질 게이트이며 외부 benchmark 주장이 아니다. 서드파티 NLP
시스템과 비교하지 않고, 대규모 코퍼스에 대한 통계적 정확도를 게시하지도 않는다.
0.2.0 결정적 게이트가 안정화된 뒤 향후 milestone에서 더 큰 코퍼스와 점수화된
precision/recall 지표를 추가할 수 있다.

## 재현 방법

저장소 루트에서 JDK 21+와 체크인된 Gradle wrapper를 사용해 증거 매트릭스의
명령을 실행한다.

이 보고서의 검증 환경:

| 항목 | 값 |
|---|---|
| OS | macOS local workspace |
| Date | 2026-05-27 |
| JDK | 저장소 기준선에 맞는 Java 21 이상 |
| Gradle | 체크인된 wrapper, 로컬 출력에서 Gradle 9.5.1 확인 |
| Commands | 대상 증거 매트릭스와 `./gradlew compileTestKotlin`, `./gradlew test` |
