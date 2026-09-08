# Issue #331 정규식 한 글자 match와 zero-length match 구분

## 배경

`text-search`의 `TextRedactor`는 정규식 match의 `IntRange`를 원문 UTF-16
half-open span으로 변환한다. Workshop의 `SensitiveTextRedactionPipeline`도
이 provider를 그대로 위임하므로, provider가 한 글자 정규식과 원문 offset을
정확히 처리해야 두 모듈의 redaction 결과가 일치한다.

## 원인

기존 조건은 `match.range.first >= match.range.last`였다. Kotlin `Regex`에서
한 글자 match의 범위는 `n..n`이므로 유효한 match인데도 이 조건에 걸려
제거되었다. 반면 zero-length match의 범위는 `n..n-1`이므로 실제로 비어
있으며 제거되어야 한다.

## 결정

정규식 match는 `match.range.isEmpty()`인 경우에만 제외한다. 유효한 범위는
기존처럼 `last + 1`을 exclusive end로 변환해 원문 UTF-16 offset과
`RedactionRange`의 half-open 계약을 유지한다.

## 결과

ASCII와 Hangul 한 글자 정규식이 마스킹되고, 겹치는 match는 하나의 span으로
병합되며, adjacent span은 분리된다. zero-length 정규식은 계속 무시된다.

## 검증

- RED 단계에서 `RedactionTest` 9개 중 2개가 한 글자 정규식 누락을 재현했다.
- 회귀 테스트는 ASCII/Hangul 결과, 원문 offset, `matchedLength`, overlap merge,
  adjacent span 분리, zero-length 무시를 검증한다.
- Workshop 소비자는 `SensitiveTextRedactionPipeline`에서 공용 `TextRedactor`에
  위임하므로 provider 수정 후 기존 `SensitiveTextRedactionPipelineTest`의
  redaction·range 검증을 함께 실행해야 한다.

## 재사용 지침

정규식 `MatchResult.range`를 span으로 변환할 때 endpoint의 대소 비교로
비어 있지 않은 한 글자 범위를 제거하지 않는다. `isEmpty()`로 empty range만
판별하고, 변환 뒤에는 `last + 1`과 half-open offset 계약을 함께 검증한다.

## 관련 항목

- Issue #331

## 최종 검증

- `:text-search:test :text-search:detekt`: 107개 테스트와 정적 검사 통과.
- Workshop `SensitiveTextRedactionPipelineTest` 20개와 임시 단일 문자 소비자 회귀 테스트 1개 통과. test runtime classpath 맨 앞에 이 worktree의 provider 클래스를 주입하고 `TextRedactor`의 `codeSource.location`으로 실제 로드 경로까지 확인했다.
- 소비자 회귀는 `7 한` → `* *`, 원본 범위 `[0,1)`, `[2,3)`, 각 `matchedLength=1`과 `ruleIds`를 검증했다. Workshop 추적 소스와 dependency catalog는 변경하지 않았다.
- `javap -public`으로 기존 `TextRedactor` 공개 JVM 서명과 동일함을 확인했다.
