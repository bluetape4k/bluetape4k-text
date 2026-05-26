# WIP - bluetape4k-text

Snapshot: 2026-05-24 KST
Scope: patch/minor milestone discovery plus open GitHub issue queue.
Open count: 0 issues.

## 2026-05-24 Milestone Refresh

Current evidence: latest tags `0.1.2`, `0.1.1`, `0.1.0`. GitHub has no open
issues; milestones `0.1.1` and `0.1.2` are clear, and `backlog` is empty.

| Lane | Candidate milestone | Current candidates | Decision |
|---|---|---|---|
| Patch | `0.1.3` | none yet | Do not invent patch work. Use only for tokenizer/runtime regressions, dependency pins, CI failures, or release-doc drift. |
| Minor | `0.2.0` | discovery needed | Start with benchmark/provider-quality discovery before creating feature issues. Candidate themes: tokenizer accuracy fixtures, dictionary update pipeline, language detection quality, and README visual/report refresh. |

Recommended order: keep patch lane empty; create one `0.2.0` discovery issue
before implementation issues; prefer measurable quality/benchmark work over
broad API expansion.

## New Milestone Queue - 2026-05-24

### New patch milestone `0.1.3`

- No issue yet. Keep this patch lane empty until a concrete regression appears.

### New minor milestone `0.2.0`

1. [#83](https://github.com/bluetape4k/bluetape4k-text/issues/83)
   `research: define text quality benchmark and fixture corpus for 0.2.0`
2. [#84](https://github.com/bluetape4k/bluetape4k-text/issues/84)
   `test: add tokenizer accuracy fixtures for Korean/Japanese mixed text`
3. [#85](https://github.com/bluetape4k/bluetape4k-text/issues/85)
   `feat: define dictionary update pipeline for tokenizer and block-word data`
4. [#86](https://github.com/bluetape4k/bluetape4k-text/issues/86)
   `docs: publish README quality benchmark report for tokenizer and language detection`

## Issue Discovery - 2026-05-24

Patch candidates:

- None currently. Do not create patch issues without a concrete regression.

Minor candidates:

- `research: define text quality benchmark and fixture corpus for 0.2.0`
- `test: add tokenizer accuracy fixtures for Korean/Japanese mixed text`
- `feat: define dictionary update pipeline for tokenizer/block-word data`
- `docs: publish README quality/benchmark report for tokenizer and language detection`

## Recently Completed

- CI, Nightly, snapshot, release, and code-quality workflows are merged.
- `bluetape4k-text-bom` and localized BOM README files are merged.
- Kluent tests were migrated to `bluetape4k-assertions`.
- Lessons guidance, Kover policy, Dependabot governance, NMCP version, and
  compatibility guard maintenance are merged through PR #10 through PR #19.
- 0.1.0 release-prep, KDoc conversion, serialVersionUID cleanup, TODO cleanup,
  Maven Central publishing configuration, and release-note work are closed.
- The 2026-05-17 security, correctness, performance, and test backlog (#31
  through #49) is closed and captured in `docs/lessons/`.
- `matchesAsFlow()` streaming contract fix for #67 is merged.

## Current Direction

0.1.1 release preparation.

The 0.1.1 release gate is clear after #67. Keep future feature work out of the
patch release.

## Priority Queue

| Priority | Issue | Difficulty | Notes |
|---|---|---:|---|
| P1 | Next assigned issue | TBD | No assigned open issue remains after #67 merges. |

## WIP Limits

| Lane | Limit | Current next |
|---|---:|---|
| Correctness / performance | 1 | Wait for the next assigned issue. |
| Feature work | 1 | Wait for a new assigned feature issue after `#67`. |
| Build/CI maintenance | 1 | Handle only concrete failures from CI/Nightly. |
| Docs/KDoc polish | 1 | Keep small and separate from tokenizer behavior changes. |
