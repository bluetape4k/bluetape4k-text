# Issue 97 Aho-Corasick Benchmark Baselines

## Context

Issue #97 needed measurable 0.2.1 baselines for `text-search` without changing
production APIs.

## Decision

Keep the benchmark in the existing `text-search/src/benchmark` kotlinx-benchmark
harness and split the scenarios by risk surface: large dictionary, Flow
collection, dense matches, no-match input, Unicode normalization, and a small
naive `String.contains` comparison.

## Outcome

The benchmark now produces six throughput snapshots and preserves the raw JMH
JSON under `docs/benchmark/`.

## Verification

- `./gradlew :text-search:compileBenchmarkKotlin :text-search:test`
- `./gradlew :text-search:benchmark`
- `jq -e 'length == 6 and all(.[]; .primaryMetric.scoreUnit == "ops/s" and .mode == "thrpt")' docs/benchmark/2026-06-04-issue-97-ahocorasick-baselines.json`
- `git diff --check`

## Future Notes

When comparing benchmark deltas, reuse the Gradle `:text-search:benchmark`
task and document that higher `ops/s` is better. Treat local short-window runs
as comparable snapshots, not production rankings.
