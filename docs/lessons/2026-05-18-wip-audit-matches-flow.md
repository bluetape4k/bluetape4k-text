# 2026-05-18 — Text WIP audit and matchesAsFlow contract

## Context

The repository had no assigned open issues, while qmd surfaced an older
text-search performance note from the pre-split `bluetape4k-projects` docs.
Live GitHub state showed the 2026-05-17 bug, security, test, and release-prep
queue had already been closed.

## Decision

Register #67 for the remaining `matchesAsFlow()` contract gap. The function
claims backpressure-friendly streaming and recommends `take(1)`, but it calls
eager `parseText(text)` before emitting any `Flow` item.

## Outcome

`WIP.md` now lists one open assigned issue, #67, as the next
correctness/performance item for `text-search`.

## Verification

- `gh issue list --state open --assignee debop` returned one open issue.
- `gh issue view 67` confirmed #67 is open, labelled `bug`, `performance`, and
  `text-search`, and assigned to `debop`.
- `rg` confirmed #67 and the open count are present in `WIP.md`.

## Future Agents

For text-search Flow work, validate both behavioral results and allocation or
early-cancellation shape. A passing match list alone does not prove the Flow
contract.
