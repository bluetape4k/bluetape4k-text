# 품질 게이트

배포 품질 근거는 저장소에서 결정적으로 재현하는 검사다. 일부 안정 동작을 증명하지만 NLP 정확도 전반에 대한 통계적 주장은 아니다.

## 검증 범위

| 영역 | 안정 근거 |
|---|---|
| 한국어 혼합 텍스트 토큰화 | `KoreanTextProcessorTest`의 surface token fixture |
| 일본어 혼합 텍스트 토큰화 | `JapaneseProcessorTest` fixture |
| 혼합 언어 감지 | `LanguageDetectorExtensionsTest` |
| 요청 경계 실패 | `TokenizeMessageTest`, `BlockMessageTest`, 프로세서 facade 테스트 |
| 안전 adapter | 제출 원문을 숨기는 실행 예제 테스트 |

fixture는 사용자에게 보이는 token surface에 집중한다. 내부 형태소 선택 전체를 고정하지 않으므로 모델과 사전이 개선돼도 실제 사용자 결과가 바뀐 경우만 회귀로 드러난다.

## 재현하기

```bash
./gradlew :tokenizer-korean:test \
  --tests "io.bluetape4k.tokenizer.korean.KoreanTextProcessorTest"

./gradlew :tokenizer-japanese:test \
  --tests "io.bluetape4k.tokenizer.japanese.JapaneseProcessorTest"

./gradlew :lingua:test \
  --tests "io.bluetape4k.lingua.LanguageDetectorExtensionsTest"
```

보고서는 macOS, JDK 21 이상, 저장소 Gradle wrapper 환경에서 검증됐으며 당시 로컬 출력에서는 Gradle 9.5.1이 확인됐다.

## 증명하지 않는 내용

이 보고서는 다음을 증명하지 않는다.

- 다른 NLP 시스템과의 정확도 비교
- 대규모 외부 corpus의 precision, recall, F1
- 다른 host의 성능과 메모리
- 기본 금칙어 사전이 모든 제품 정책에 맞는지
- 각 애플리케이션의 지원 언어와 입력 분포

배포 판단에서 이 한계를 함께 밝혀야 한다. 결정적 게이트의 가치는 모든 질문을 답하는 데 있지 않고 같은 조건에서 재현할 수 있다는 데 있다.

## 게이트 확장하기

사용자에게 보이는 token, route, masking 결과, 정제된 오류가 제품 계약이 되면 fixture를 추가한다. 통계 corpus 평가를 추가할 때는 corpus 버전, 지표, 재현 명령, 해석 임계값을 함께 관리한다.

[테스트](../guides/testing.md)에서 애플리케이션 검증을, [Aho-Corasick 벤치마크](aho-corasick-benchmarks.md)에서 성능 근거를 확인하자.

## 소스 근거

- [품질 보고서](https://github.com/bluetape4k/bluetape4k-text/blob/0.3.0/docs/superpowers/research/2026-05-27-issue-86-quality-report.md)
- [품질 벤치마크 명세](https://github.com/bluetape4k/bluetape4k-text/blob/0.3.0/docs/superpowers/specs/2026-05-27-issue-83-text-quality-benchmark-spec.md)
