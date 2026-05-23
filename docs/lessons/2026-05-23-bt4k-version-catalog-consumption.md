# bt4k Version Catalog Consumption

## Context

`bluetape4k-text` had a local shared version pin that should follow the
ecosystem catalog.

## Decision

Import the shared `bt4k` version catalog and use `bt4kVersion(alias)` for shared
leaf dependency constraints.

## Outcome

The selected shared dependency alias is versionless locally, and dependency
management supplies its version from `bluetape4k-dependencies`.

## Verification

- `git diff --check`
- `./gradlew help --no-daemon --no-configuration-cache`
- `./gradlew compileKotlin --no-daemon --no-configuration-cache`

## Future Guidance

Keep text-specific library choices local, but do not pin shared version values
that already exist in `bt4k`.
