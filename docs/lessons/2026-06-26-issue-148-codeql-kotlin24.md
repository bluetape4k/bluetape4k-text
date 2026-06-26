# Issue #148 CodeQL Kotlin 2.4

## Context

Code Quality Analysis failed on `Analyze (java-kotlin)` after the repository
moved to Kotlin 2.4.0. The failure message came from CodeQL's Kotlin extractor:
Kotlin 2.4.0 is newer than the supported extractor window. The same `develop`
head had green Nightly, CI, and Publish Snapshot runs.

## Decision

Pause only the CodeQL `java-kotlin` matrix axis until CodeQL supports Kotlin
2.4.x. Keep `actions` scanning active so workflow analysis still runs.

## Outcome

The Code Quality workflow no longer schedules the unsupported Kotlin extractor
path. The dormant Java/Kotlin path now uses `assemble` for the future re-enable
case so it remains compile-only.

## Verification

- `actionlint .github/workflows/code-quality.yml`
- `./gradlew assemble --no-daemon`
- `git diff --check`

## Future Guidance

Do not downgrade Kotlin to satisfy CodeQL scanner lag. Re-enable `java-kotlin`
only after CodeQL documents support for Kotlin 2.4.x, and keep the re-enabled
Gradle command compile-only.
