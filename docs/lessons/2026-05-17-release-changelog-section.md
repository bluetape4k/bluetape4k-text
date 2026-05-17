# Release Changelog Section

## Context

The 0.1.0 release preflight found that `CHANGELOG.md` still kept release notes
under `Unreleased`, so the release workflow would fall back to generic GitHub
release notes.

## Decision

Move the prepared notes under `## [0.1.0] - 2026-05-17` before tagging.

## Outcome

The release workflow can now extract version-specific notes for GitHub Release
creation.

## Verification

- Checked `CHANGELOG.md` for the `0.1.0` section.

## Future Guidance

Before tagging a release, verify that `CHANGELOG.md` has a section matching
`baseVersion`.
