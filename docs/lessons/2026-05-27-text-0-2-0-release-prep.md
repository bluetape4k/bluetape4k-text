# Text 0.2.0 Release Prep

## Context

The 0.2.0 milestone has no open issues after PR #106 closed the quality-gate
work for benchmark fixtures, dictionary-update governance, README benchmark
reporting, and validation/security regression coverage.

## Decision

Prepare `bluetape4k-text` 0.2.0 as a stable release from `develop`, keeping the
release-prep diff limited to version metadata, CHANGELOG, and this lesson.

## Outcome

Release metadata now uses `baseVersion=0.2.0`, and the CHANGELOG documents the
0.2.0 quality-gate scope before tagging.

## Verification

Run the release preflight before tagging: Gradle metadata check, workflow lint,
publication POM generation, stale/SNAPSHOT POM scan, local Maven publication,
and PR CI.

## Future Notes

Do not tag a stable release until `baseVersion` exactly matches the tag and
`snapshotVersion` is empty; the release workflow enforces both checks.
