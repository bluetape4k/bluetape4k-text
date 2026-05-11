# Kover Coverage Policy

## Current Status

`bluetape4k-text` generates Kover XML reports in Nightly. `text-search`
excludes benchmark packages from coverage measurement. No module currently
enforces `koverVerify`.

## Policy

Status: report-only transition.

Tokenizer and text-search modules are good candidates for strict coverage
because much of the behavior is deterministic. Baselines still need to be
recorded before failing gates are enabled.

## Threshold Plan

- Measure tokenizer-core, tokenizer-korean, tokenizer-japanese, lingua, and
  text-search coverage from Nightly artifacts.
- Add module-level 80% gates where the baseline supports it.
- Keep benchmark packages excluded.

## CI/Nightly Contract

Nightly uploads Kover XML artifacts. Add `koverVerify` after module baselines
are accepted.
