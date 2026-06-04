# Nightly Snapshot Cache Refresh

## Context

Nightly failed on the same `develop` SHA while different module test jobs tried
to resolve `io.github.bluetape4k:*:1.11.0-SNAPSHOT` dependencies.

## Decision

Refresh dependencies in Nightly Gradle invocations so restored Gradle caches do
not reuse stale Central snapshot metadata.

## Outcome

The workflow keeps Gradle caching, but each Nightly build, detekt, test, and
Kover report call asks Gradle to re-check dependency metadata.

## Verification

- `actionlint .github/workflows/nightly-tests.yml`
- `git diff --check`
- `./gradlew build -x test --parallel --refresh-dependencies --no-daemon`
- `./gradlew :tokenizer-core:test :tokenizer-japanese:test :tokenizer-korean:test :lingua:test :text-search:test --no-daemon --refresh-dependencies`
- `./gradlew detekt --parallel --refresh-dependencies --no-daemon`
- `./gradlew :tokenizer-japanese:koverXmlReport --no-daemon --refresh-dependencies`

## Future Guidance

When Nightly consumes Central snapshots, do not trust restored Gradle metadata
as the only source of truth. Add `--refresh-dependencies` to snapshot-sensitive
Nightly calls before treating module test failures as source-code failures.
