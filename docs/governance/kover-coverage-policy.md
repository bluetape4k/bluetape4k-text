# Kover Coverage Policy

## Current Status

`bluetape4k-text` generates Kover XML reports in Nightly. `text-search`
excludes benchmark packages from coverage measurement. No module currently
enforces a failing coverage threshold.

## Policy

Status: report-only transition.

Tokenizer and text-search modules are good candidates for close coverage
monitoring because much of the behavior is deterministic. Coverage should still
remain a trend signal unless a focused issue explicitly reintroduces a gate.

## Threshold Plan

- Measure tokenizer-core, tokenizer-korean, tokenizer-japanese, lingua, and
  text-search coverage from Nightly artifacts.
- Use Nightly XML reports and existing coverage artifact uploads to identify
  coverage regressions.
- Open a focused issue when a module needs coverage repair; do not introduce a
  failing threshold as the default enforcement mechanism.
- Keep benchmark packages excluded.

## CI/Nightly Contract

Nightly uploads Kover XML artifacts and keeps trend visibility. CI and Nightly
must not fail solely because a module is below a fixed coverage percentage
unless a future issue explicitly reintroduces that gate.
