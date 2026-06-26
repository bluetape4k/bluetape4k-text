# Issue #148 CodeQL Kotlin 2.4 Review

Date: 2026-06-26
Scope: `.github/workflows/code-quality.yml`
Issue: #148

## Review Result

- P0: 0
- P1: 0
- P2/P3: none

## Evidence

- Code Quality Analysis run `28214748358` failed only on
  `Analyze (java-kotlin)`; `Analyze (actions)` passed.
- The same `develop` head had green Nightly, CI, and Publish Snapshot runs, so
  this is CodeQL extractor support-window lag rather than a product regression.
- The workflow matrix now keeps `actions` enabled and pauses only
  `java-kotlin` until CodeQL supports Kotlin 2.4.x.
- The dormant `java-kotlin` build command is `assemble`, so a future re-enable
  remains compile-only.

## Validation

- `actionlint .github/workflows/code-quality.yml`: PASS
- `./gradlew assemble --no-daemon`: PASS
- `git diff --check`: PASS

## Gate Verdict

PASS. The change is limited to the intended CodeQL workflow surface, preserves
GitHub Actions scanning, and records the re-enable condition in workflow
comments.
