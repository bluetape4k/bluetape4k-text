# Issue #157 lingua coverage review

## Scope

- Repository: bluetape4k-text
- Issue: #157 `test(lingua): raise coverage above repo module average`
- Module: `:lingua`
- Target: exceed repository module average 81.64% instruction coverage.

## Changes reviewed

- Added direct Unicode block coverage tests for `Char.isAscii`, `isLatin`, `isArabic`, `isThai`, `isKorean`, `isJapanese`, and `isChinese`.
- Added `UnicodeDetector` tests for supported locales, `filterChar`, and `filterString` locale filtering.
- Added `LanguageDetector` builder/extension tests for all-spoken detector construction, low-accuracy overloads, Latin phrase candidate correction, single-letter Latin-token filtering, and non-preferred long Latin-token filtering in mixed text.

## Coverage evidence

- Baseline from `lingua/build/reports/kover/report.xml`: 816/1120 instructions = 72.86%.
- After tests: 1005/1120 instructions = 89.73%.
- Result: PASS, module exceeds the 81.64% repo module average by +8.09 percentage points.

## Verification

- PASS: `./gradlew :lingua:test :lingua:koverXmlReport --no-daemon --no-configuration-cache`
- PASS: `./gradlew :lingua:check :lingua:koverXmlReport --no-daemon --no-configuration-cache`

## Review verdict

- P0/P1 findings: none.
- Public API changes: none.
- Production behavior changes: none.
- Testcontainers/concurrency helpers: not applicable; this module uses pure JVM language-detection/unit tests.
