# Nightly Snapshot Cache Refresh

## Context

Nightly failed on the same `develop` SHA while different module test jobs tried
to resolve `io.github.bluetape4k:*:1.11.0-SNAPSHOT` dependencies.
The cross-repository follow-up also reproduced Central snapshot `403` when
several downstream Nightly workflows were manually dispatched at the same time.

## Decision

Refresh dependencies in Nightly Gradle invocations so restored Gradle caches do
not reuse stale Central snapshot metadata.
Stagger the scheduled Nightly cron minute across downstream repositories so
scheduled runs do not all request mutable Central snapshot metadata at once.

## Outcome

The workflow keeps Gradle caching, but each Nightly build, detekt, test, and
Kover report call asks Gradle to re-check dependency metadata.
The scheduled `bluetape4k-text` Nightly start minute is separated from the other
downstream snapshot-consuming repositories.

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
Keep scheduled downstream Nightly start minutes staggered instead of putting
every snapshot-consuming repository on the same cron minute.
