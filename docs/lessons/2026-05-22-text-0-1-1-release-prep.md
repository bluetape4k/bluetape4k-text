# Text 0.1.1 Release Prep

## Context

The 0.1.1 milestone has no open issues after the `matchesAsFlow()` streaming
fix. The repository is still on the 0.1.1 snapshot line and references
`bluetape4k-bom:1.8.0`.

## Decision

Prepare `bluetape4k-text` 0.1.1 as a release and align it with
`bluetape4k-bom:1.9.0`.

## Outcome

Release metadata, dependency catalog, CHANGELOG, and WIP were updated for the
0.1.1 release gate.

## Verification

Verified the release version, GitHub Actions workflow syntax, publication POM
generation, stale/snapshot POM absence, and local Maven publication before
opening the release PR.

## Future Notes

Do not add backlog feature work to the 0.1.1 patch release after #67 has closed.
