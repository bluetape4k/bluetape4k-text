# Changelog

All notable changes to `bluetape4k-text` are documented here.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).
This project follows [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Fixed

- `matchesAsFlow()` now streams default Aho-Corasick matches from trie traversal and stops cooperatively when collectors cancel with `take(N)`; overlap and word-boundary post-processing still uses the eager filtered path ([#67](https://github.com/bluetape4k/bluetape4k-text/issues/67)).

## [0.1.0] - 2026-05-17

### Added

- Root README hero image plus refreshed project-purpose, feature, and language-switch entrypoint documentation ([PR #20](https://github.com/bluetape4k/bluetape4k-text/pull/20)).
- GitHub Actions workflows for CI, nightly, snapshot, release, and code-quality checks ([PR #2](https://github.com/bluetape4k/bluetape4k-text/pull/2)).
- `bluetape4k-text-bom` BOM module for text library consumers ([PR #7](https://github.com/bluetape4k/bluetape4k-text/pull/7)).
- English and Korean README files for the text BOM module ([PR #8](https://github.com/bluetape4k/bluetape4k-text/pull/8)).

### Changed

- Normalized lessons, Kover, Dependabot, NMCP, compatibility guards, and dependency maintenance ([PR #10](https://github.com/bluetape4k/bluetape4k-text/pull/10), [PR #11](https://github.com/bluetape4k/bluetape4k-text/pull/11), [PR #12](https://github.com/bluetape4k/bluetape4k-text/pull/12), [PR #16](https://github.com/bluetape4k/bluetape4k-text/pull/16), [PR #17](https://github.com/bluetape4k/bluetape4k-text/pull/17), [PR #18](https://github.com/bluetape4k/bluetape4k-text/pull/18), [PR #19](https://github.com/bluetape4k/bluetape4k-text/pull/19)).
- Updated text dependency catalog and dependency bumps, including GitHub Actions and annotations ([PR #9](https://github.com/bluetape4k/bluetape4k-text/pull/9), [PR #14](https://github.com/bluetape4k/bluetape4k-text/pull/14), [PR #15](https://github.com/bluetape4k/bluetape4k-text/pull/15)).
- CI uses path filtering and retry configuration ([PR #5](https://github.com/bluetape4k/bluetape4k-text/pull/5)).
- Test code migrated from Kluent to `bluetape4k-assertions` ([PR #6](https://github.com/bluetape4k/bluetape4k-text/pull/6)).
