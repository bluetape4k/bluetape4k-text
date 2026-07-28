# 2026-06-04 Issue 130 Nightly Gradle Cache

## 배경

bluetape4k repository들의 Nightly build가 GitHub runner에서 managed dependency를
간헐적으로 `group:artifact:.` 형태로 resolve했다.

## 결정

Scheduled run이 오래된 dependency-management state를 재사용하지 않도록 Nightly job에서
`gradle/actions/setup-gradle` cache restore/write를 비활성화한다.

## 결과

모든 Nightly `setup-gradle` block은 명시적 Gradle dependency refresh를 유지하면서
`cache-disabled: true`를 설정한다.

## 검증

- `.github/workflows/nightly-tests.yml`을 audit했다. `setup-gradle` block은
  `cache-disabled` block과 일치한다.
- 계획된 검증: `actionlint`, `git diff --check`.

## 향후 규칙

Nightly workflow가 snapshot 또는 BOM-managed bluetape4k dependency를 사용할 때는,
cache restore가 오래된 metadata를 replay하지 않는다는 최신 CI 근거가 없으면 Gradle action
cache를 비활성화한 상태로 둔다.
