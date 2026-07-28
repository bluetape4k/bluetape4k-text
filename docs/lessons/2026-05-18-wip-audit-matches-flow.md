# 2026-05-18 — Text WIP audit와 matchesAsFlow contract

## 배경

저장소에는 할당된 open issue가 없었지만, qmd는 분리 전 `bluetape4k-projects` 문서에
있던 오래된 text-search performance note를 보여줬다. Live GitHub state에서는
2026-05-17 bug, security, test, release-prep queue가 이미 닫혀 있었다.

## 결정

남은 `matchesAsFlow()` contract gap을 #67로 등록한다. 이 함수는 backpressure-friendly
streaming을 주장하고 `take(1)`을 권장하지만, `Flow` item을 emit하기 전에 eager
`parseText(text)`를 호출한다.

## 결과

`WIP.md`는 이제 `text-search`의 다음 correctness/performance item으로 open assigned
issue #67 하나를 나열한다.

## 검증

- `gh issue list --state open --assignee debop`는 open issue 하나를 반환했다.
- `gh issue view 67`로 #67이 open이고 `bug`, `performance`, `text-search` label과
  assignee `debop`을 가진 것을 확인했다.
- `rg`로 #67과 open count가 `WIP.md`에 있음을 확인했다.

## 향후 agent 지침

Text-search Flow 작업에서는 동작 결과와 allocation 또는 early-cancellation 형태를
모두 검증한다. Passing match list만으로는 Flow contract를 증명할 수 없다.
