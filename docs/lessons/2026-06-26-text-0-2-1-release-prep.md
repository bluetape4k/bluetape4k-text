# Text 0.2.1 Release Prep

## Context

The 0.2.1 milestone has no open issues after PR #150 closed the runnable
examples, web-service safety documentation, and Lingua coverage work.

## Decision

Prepare `bluetape4k-text` 0.2.1 as a patch stable release from `develop`.
Keep the release-prep diff limited to version metadata, release notes, the
stable catalog default, and this lesson.

## Outcome

Release metadata now uses `baseVersion=0.2.1`, `snapshotVersion=` remains empty,
the release workflow's checked-in catalog default points to
`catalog/2026-06-01-01`, and the local `bluetape4k-bom` reference uses the
Maven Central visible `1.10.0` line. The BOM constraints also exclude runnable
example projects so `bluetape4k-text-bom` publishes only stable library modules.

## Verification

Before tagging or dispatching release, verify local Gradle metadata,
publication POM generation, stale/SNAPSHOT POM absence, local Maven
publication, current PR CI, and a fresh Nightly plus snapshot validation run for
the release-prep commit.

## Future Notes

Do not tag or dispatch the stable release while the checked-in release catalog,
local version catalog, generated POMs, or release workflow inputs can resolve a
`-SNAPSHOT` dependency.
