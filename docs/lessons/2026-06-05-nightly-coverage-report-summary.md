# Nightly Coverage Report Summary

## Context

Nightly run `26958373293` uploaded module-level Kover XML artifacts, but the
`Coverage Report` job only downloaded and re-uploaded them as `coverage-all`.
The job succeeded without writing a visible GitHub Step Summary coverage table.

## Decision

Mirror the proven projects/exposed pattern: checkout the repository in the
coverage aggregation job, run a small Kover XML aggregation script, and write a
module summary to `$GITHUB_STEP_SUMMARY`.

The text workflow also validates the expected coverage artifact names before
aggregation. A missing artifact or missing `report.xml`/`reportJvm.xml` now
fails the `Coverage Report` job instead of silently publishing an empty wrapper
artifact.

## Outcome

Nightly coverage artifacts stay downloadable through `coverage-all`, and the
job summary now shows per-module line and instruction coverage.

## Verification

- `python3 .github/scripts/aggregate-kover-coverage.py <coverage-all>` against
  run `26958373293` coverage artifacts.
- `actionlint .github/workflows/nightly-tests.yml`.
- `git diff --check`.

## Future Guidance

When adding or renaming a text module, update both the test job artifact name
and the `Validate coverage artifacts` expected list in `nightly-tests.yml`.
