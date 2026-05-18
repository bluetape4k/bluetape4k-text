# WIP - bluetape4k-text

Snapshot: 2026-05-18 KST
Scope: open GitHub issues assigned to `debop`, created on or after 2026-01-01.
Open count: 1 issue.

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

## Current Direction

Text-search streaming contract correction.

`matchesAsFlow()` currently documents backpressure and `take(N)` early
termination, but it calls eager `parseText(text)` before emitting. Keep this
work focused on preserving existing Aho-Corasick match semantics while making
the Flow contract honest or truly streaming.

## Priority Queue

| Priority | Issue | Difficulty | Notes |
|---|---|---:|---|
| P1 | [#67](https://github.com/bluetape4k/bluetape4k-text/issues/67) `matchesAsFlow` materializes all Aho-Corasick matches before emitting | M | Fix streaming/backpressure contract or revise public KDoc; preserve match ordering and offset behavior. |

## WIP Limits

| Lane | Limit | Current next |
|---|---:|---|
| Correctness / performance | 1 | `#67` |
| Feature work | 1 | Wait for a new assigned feature issue after `#67`. |
| Build/CI maintenance | 1 | Handle only concrete failures from CI/Nightly. |
| Docs/KDoc polish | 1 | Keep small and separate from tokenizer behavior changes. |
