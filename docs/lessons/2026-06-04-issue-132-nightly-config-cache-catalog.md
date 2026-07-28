# 2026-06-04 Issue 132 Nightly Config Cache And Catalog

## 배경

Nightly workflow는 snapshot과 BOM-managed dependency를 사용하므로, 오래된 Gradle 또는
configuration state가 version 없는 dependency coordinate를 드러낼 수 있다.

## 결정

Nightly Gradle command에는 `--no-configuration-cache`를 유지하고, local bluetape4k
alias는 해당 BOM ref를 통해 version을 갖게 한다.

## 결과

Nightly command는 dependency refresh 중 configuration cache에 의존하지 않으며,
repo-local catalog alias는 `group:artifact:.` coordinate를 피한다.

## 검증

- 계획: `actionlint`, `git diff --check`, command audit, catalog alias audit.

## 향후 규칙

Snapshot을 refresh하는 Nightly job은 repository별 반대 근거가 없으면 Gradle action cache와
configuration cache를 모두 비활성화한다.
