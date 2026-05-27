# Text 0.2.0 Post-Release Reopen

## Context

The 0.2.0 release workflow succeeded from tag `0.2.0`, and the repository
continues development on `develop` with open 0.2.1 patch work.

## Decision

Advance `baseVersion` to `0.2.1` after the stable release and keep
`snapshotVersion=` empty, matching the shared bluetape4k release-line policy.

## Outcome

The development branch is reopened for the 0.2.1 patch line without changing
published 0.2.0 release metadata.

## Verification

Confirm Gradle reports `version: 0.2.1`, `snapshotVersion:` is empty, and PR CI
passes before merging the reopen PR.

## Future Notes

Use the next open patch milestone for post-release reopen unless the patch line
is explicitly closed or deferred.
