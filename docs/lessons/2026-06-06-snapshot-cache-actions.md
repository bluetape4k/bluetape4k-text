# Snapshot Cache Actions

## 배경

Repository가 Central snapshot의 mutable bluetape4k SNAPSHOT artifact에 의존하는 동안
Nightly가 dependency refresh를 강제했다.

## 결정

`--refresh-dependencies`를 제거하고, Nightly `cache-disabled: true`를 제거하며, root
changing-module cache TTL을 0초에서 1일로 바꾼다.

## 결과

Nightly는 기존 tokenizer와 text module task 구조를 유지한다. 다만 일반 dependency
resolution은 모든 job에서 Central snapshot metadata request를 강제하지 않고 Gradle cache
metadata를 사용할 수 있다.

## 검증

- `actionlint .github/workflows/*.yml`
- `rg -n -- '--refresh-dependencies|cache-disabled: true' .github/workflows` -> no matches
- `./gradlew help --no-daemon`
- `git diff --check`

## 향후 지침

명시적 dependency refresh는 dedicated post-publish freshness check에서만 사용한다. 일반
CI, Nightly, Examples workflow는 cached changing-module metadata에 의존하고, test-only
SNAPSHOT dependency에 필요할 때만 선별 warm-up 단계를 더한다.
