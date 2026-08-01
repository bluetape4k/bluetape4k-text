# 혼합 언어 구간 API 설계 검토

## 계약

`LanguageDetector.detectLanguageSegments`는 입력을 유니코드 문자 토큰으로 나누고, 각 토큰을 스크립트 힌트 또는 Lingua 모델로 판정한다. 반환 구간은 원문의 UTF-16 인덱스(`start`, `endExclusive`)로 표현하므로 `text.substring(start, endExclusive)`로 원문을 복원할 수 있다.

## 스크립트와 신뢰도

- 한글은 `Language.KOREAN`, 가나는 `Language.JAPANESE`, 한자는 `Language.CHINESE`로 우선 판정하며 이 세 스크립트 힌트의 신뢰도는 `1.0`이다.
- 라틴 문자와 기타 문자 구간은 Lingua의 최상위 언어와 confidence 값을 사용한다. 짧은 한 글자 라틴 토큰은 기존 혼합 언어 정책에 따라 `UNKNOWN`으로 처리할 수 있다.
- `minimumConfidence`보다 낮은 구간은 제외한다. `0.0`을 사용하면 신뢰도 `0.0`인 `UNKNOWN` 구간도 관찰할 수 있다.
- 공백, 구두점, 이모지는 구간으로 만들지 않으며, 같은 언어라도 원문에서 떨어져 있으면 병합하지 않는다.

## 모호성

한자는 중국어와 일본어가 공유하므로 한자만 있는 구간은 중국어로 분류한다. 일본어 문맥을 보존해야 하는 소비자는 호출 전에 일본어 후보를 제한하거나 후속 문맥 규칙을 적용해야 한다. 반환 구간은 서로 겹치지 않고 입력 순서대로 정렬된다.

## 검증

영어·한국어·일본어 혼합 입력, UTF-16 위치, 구두점·이모지 제외, 짧은 토큰 confidence 필터, `UNKNOWN` 관찰 및 임계값 범위 검사를 `LanguageDetectorExtensionsTest`에 추가했다.
