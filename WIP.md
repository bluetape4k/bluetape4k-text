# WIP - bluetape4k-text

Snapshot: 2026-05-22 KST
Scope: open GitHub issues assigned to `debop`, created on or after 2026-01-01.
Open count: 0 issues after #67 merges.

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
- `matchesAsFlow()` streaming contract fix for #67 is implemented on the release branch.

## Current Direction

0.1.1 release preparation.

The 0.1.1 release gate is clear after #67. Prepare release metadata after the
fix PR merges and keep future feature work out of the patch release.

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
