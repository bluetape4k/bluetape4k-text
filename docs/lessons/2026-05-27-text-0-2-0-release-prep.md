# Text 0.2.0 릴리스 준비

## 배경

PR #106이 benchmark fixture, dictionary-update governance, README benchmark reporting,
validation/security regression coverage를 위한 quality-gate 작업을 닫은 뒤 0.2.0
마일스톤에는 open issue가 없었다.

## 결정

Release-prep diff를 version metadata, CHANGELOG, 이 lesson으로 제한한 상태에서
`develop`의 `bluetape4k-text` 0.2.0을 stable release로 준비한다.

## 결과

Release metadata는 이제 `baseVersion=0.2.0`을 사용하고, CHANGELOG는 tag 생성 전에
0.2.0 quality-gate 범위를 문서화한다.

## 검증

Tag 생성 전에 release preflight를 실행한다. 검증 항목은 Gradle metadata check,
workflow lint, publication POM generation, stale/SNAPSHOT POM scan, local Maven
publication, PR CI다.

## 향후 메모

`baseVersion`이 tag와 정확히 일치하고 `snapshotVersion`이 비어 있음을 확인하기 전에는
stable release tag를 만들지 않는다. Release workflow는 두 조건을 모두 강제한다.
