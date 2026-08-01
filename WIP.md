# WIP - bluetape4k-text

스냅샷: 2026-08-01 KST
범위: 0.3.0 릴리스 트레인의 CI·문서 정리와 열린 백로그 관리.
열린 이슈 수: 6개.

## 2026-08-01 GitHub 상태 갱신

현재 근거: GitHub 기준 열린 이슈는 6개다. `0.3.0`에는 #227 문서 갱신이
진행 중이다. #140 gitleaks 설치 강화와 #225 CHANGELOG 용어 표준화는
각각 PR #165와 PR #226에서 완료됐다. 나머지 #104, #105, #116, #117, #118은
백로그에 남아 있다.

### 진행 중

- [#227](https://github.com/bluetape4k/bluetape4k-text/issues/227)
  `docs: refresh WIP and CHANGELOG for current 0.3.0 queue` — 현재 GitHub
  이슈 대기열과 완료된 유지보수 작업을 WIP/CHANGELOG에 반영한다.

### 최근 완료

- [#140](https://github.com/bluetape4k/bluetape4k-text/issues/140)의 gitleaks
  설치 강화를 [PR #165](https://github.com/bluetape4k/bluetape4k-text/pull/165)에서 완료했다.
- [#225](https://github.com/bluetape4k/bluetape4k-text/issues/225)의 한국어
  `Fixed` 범주 표준화를 [PR #226](https://github.com/bluetape4k/bluetape4k-text/pull/226)에서
  완료했다.

### 백로그

1. [#104](https://github.com/bluetape4k/bluetape4k-text/issues/104)
   `build: add release metadata smoke check against Maven Central before handoff`
2. [#105](https://github.com/bluetape4k/bluetape4k-text/issues/105)
   `feat: evaluate modern Japanese tokenizer backend beyond Kuromoji IPADic`
3. [#116](https://github.com/bluetape4k/bluetape4k-text/issues/116)
   `examples: evaluate Japanese tokenizer backend comparison sample`
4. [#117](https://github.com/bluetape4k/bluetape4k-text/issues/117)
   `examples: add offline corpus quality report generator sample`
5. [#118](https://github.com/bluetape4k/bluetape4k-text/issues/118)
   `epic: build runnable bluetape4k-text examples suite`

## 2026-05-24 마일스톤 갱신

현재 근거: 최신 태그는 `0.1.2`, `0.1.1`, `0.1.0`이다. GitHub에는 열린
이슈가 없고, `0.1.1`과 `0.1.2` 마일스톤은 정리됐으며 `backlog`도 비어 있다.

| Lane | 후보 마일스톤 | 현재 후보 | 결정 |
|---|---|---|---|
| Patch | `0.1.3` | 아직 없음 | 패치 작업을 억지로 만들지 않는다. tokenizer/runtime regression, dependency pin, CI failure, release-doc drift가 구체적으로 있을 때만 사용한다. |
| Minor | `0.2.0` | discovery 필요 | feature issue를 만들기 전에 benchmark/provider-quality discovery부터 시작한다. 후보 주제는 tokenizer accuracy fixture, dictionary update pipeline, language detection quality, README visual/report refresh다. |

권장 순서: patch lane은 비워 둔다. 구현 이슈보다 먼저 `0.2.0` discovery issue를
하나 만든다. 넓은 API 확장보다 측정 가능한 quality/benchmark 작업을 우선한다.

## 새 마일스톤 큐 - 2026-05-24

### 새 patch milestone `0.1.3`

- 아직 이슈가 없다. 구체적인 regression이 나타날 때까지 이 patch lane을 비워 둔다.

### 새 minor milestone `0.2.0`

1. [#83](https://github.com/bluetape4k/bluetape4k-text/issues/83)
   `research: define text quality benchmark and fixture corpus for 0.2.0`
2. [#84](https://github.com/bluetape4k/bluetape4k-text/issues/84)
   `test: add tokenizer accuracy fixtures for Korean/Japanese mixed text`
3. [#85](https://github.com/bluetape4k/bluetape4k-text/issues/85)
   `feat: define dictionary update pipeline for tokenizer and block-word data`
4. [#86](https://github.com/bluetape4k/bluetape4k-text/issues/86)
   `docs: publish README quality benchmark report for tokenizer and language detection`

## 이슈 탐색 - 2026-05-24

Patch 후보:

- 현재 없음. 구체적인 regression 없이 patch issue를 만들지 않는다.

Minor 후보:

- `research: define text quality benchmark and fixture corpus for 0.2.0`
- `test: add tokenizer accuracy fixtures for Korean/Japanese mixed text`
- `feat: define dictionary update pipeline for tokenizer/block-word data`
- `docs: publish README quality/benchmark report for tokenizer and language detection`

## 최근 완료

- CI, Nightly, snapshot, release, code-quality workflow가 merge됐다.
- `bluetape4k-text-bom`과 localized BOM README 파일이 merge됐다.
- Kluent test를 `bluetape4k-assertions`로 이전했다.
- Lessons guidance, Kover policy, Dependabot governance, NMCP version,
  compatibility guard maintenance가 PR #10부터 PR #19까지 merge됐다.
- 0.1.0 release-prep, KDoc conversion, serialVersionUID cleanup, TODO cleanup,
  Maven Central publishing configuration, release-note 작업이 닫혔다.
- 2026-05-17 security, correctness, performance, test backlog(#31부터 #49까지)가
  닫혔고 `docs/lessons/`에 기록됐다.
- #67의 `matchesAsFlow()` streaming contract fix가 merge됐다.

## 현재 방향

`0.2.0` stable line은 publish됐고 `bluetape4k-dependencies` `1.2.0`에서
소비하고 있다. 이제 개발은 `0.3.0`으로 이동하며, workflow가 주입하는 snapshot
publication을 위해 `snapshotVersion=`은 비워 둔다.

## 우선순위 큐

| Priority | Issue | 난이도 | 비고 |
|---|---|---:|---|
| P1 | [#227](https://github.com/bluetape4k/bluetape4k-text/issues/227) | 낮음 | 현재 GitHub 이슈 대기열과 #140/#225 완료 상태를 WIP와 CHANGELOG에 반영한다. |
| P2 | #104, #105, #116, #117, #118 | 중간 | 백로그 항목은 구체적인 범위와 실행 근거를 확인한 뒤 일정에 올린다. |

## WIP Limits

| Lane | 제한 | 현재 다음 작업 |
|---|---:|---|
| Correctness / performance | 1 | 구체적인 regression이 생길 때까지 `Backlog`를 유지한다. |
| Feature work | 1 | #105와 #116의 backend 평가 범위를 먼저 확정한다. |
| Build/CI maintenance | 1 | #140은 검증 완료됐고, 새 릴리스/CI 실패가 있을 때만 처리한다. |
| Docs/KDoc polish | 1 | PR #227의 WIP/CHANGELOG 갱신을 마무리한 뒤 다음 할당 이슈를 기다린다. |
