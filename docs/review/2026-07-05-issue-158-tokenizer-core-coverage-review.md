# Issue #158 tokenizer-core coverage review

## Scope

- Repository: bluetape4k-text
- Issue: #158 `test(tokenizer-core): raise coverage above repo module average`
- Module: `:tokenizer-core`
- Target: exceed repository module average 81.64% instruction coverage.

## Changes reviewed

- Added constructor coverage for tokenizer exception hierarchy.
- Added `CharArrayMap` tests for unmodifiable views, companion copy, empty singleton behavior, entry iteration, entry-set mutation guards, key-set mutation guards, and string rendering.
- Added `CharArraySet` tests for companion copy, unmodifiable views, and string rendering.
- Added `CharacterUtils` tests for `CharacterBuffer` wrapping, trailing high-surrogate carry-over, invalid fill size, and negative conversion lengths.

## Coverage evidence

- Baseline from `tokenizer-core/build/reports/kover/report.xml`: 1927/2698 instructions = 71.42%.
- After tests: 2315/2698 instructions = 85.80%.
- Result: PASS, module exceeds the 81.64% repo module average by +4.16 percentage points.

## Verification

- PASS: `./gradlew :tokenizer-core:test :tokenizer-core:koverXmlReport --no-daemon --no-configuration-cache`
- PASS: `./gradlew :tokenizer-core:check :tokenizer-core:koverXmlReport --no-daemon --no-configuration-cache`

## Review verdict

- P0/P1 findings: none.
- Public API changes: none.
- Production behavior changes: none.
- Testcontainers/concurrency helpers: not applicable; this module uses pure JVM utility/model tests.
