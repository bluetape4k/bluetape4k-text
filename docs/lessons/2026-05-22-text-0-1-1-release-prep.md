# Text 0.1.1 릴리스 준비

## 배경

`matchesAsFlow()` streaming 수정 후 0.1.1 마일스톤에는 open issue가 없었다.
저장소는 아직 0.1.1 스냅숏 라인에 있었고 `bluetape4k-bom:1.8.0`을 참조했다.

## 결정

`bluetape4k-text` 0.1.1을 릴리스로 준비하고 `bluetape4k-bom:1.9.0`에 맞춘다.

## 결과

Release metadata, dependency catalog, CHANGELOG, WIP를 0.1.1 release gate에 맞춰
업데이트했다.

## 검증

Release PR을 열기 전에 release version, GitHub Actions workflow syntax, publication POM
생성, stale/snapshot POM 부재, local Maven publication을 확인했다.

## 향후 메모

#67이 닫힌 뒤에는 0.1.1 patch release에 backlog feature work를 추가하지 않는다.
