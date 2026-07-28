# Nightly Coverage Report Summary

## 배경

Nightly run `26958373293`은 module-level Kover XML artifact를 upload했지만,
`Coverage Report` job은 이를 `coverage-all`로 download한 뒤 다시 upload하기만 했다. Job은
성공했지만 GitHub Step Summary에 보이는 coverage table은 작성하지 않았다.

## 결정

검증된 projects/exposed pattern을 따른다. Coverage aggregation job에서 repository를
checkout하고, 작은 Kover XML aggregation script를 실행한 뒤 module summary를
`$GITHUB_STEP_SUMMARY`에 작성한다.

Text workflow는 aggregation 전에 예상 coverage artifact name도 검증한다. Artifact가
없거나 `report.xml`/`reportJvm.xml`이 없으면 이제 빈 wrapper artifact를 조용히 publish하지
않고 `Coverage Report` job을 실패시킨다.

## 결과

Nightly coverage artifact는 `coverage-all`을 통해 계속 download할 수 있고, job summary는
module별 line coverage와 instruction coverage를 보여준다.

## 검증

- `python3 .github/scripts/aggregate-kover-coverage.py <coverage-all>` against
  run `26958373293` coverage artifacts.
- `actionlint .github/workflows/nightly-tests.yml`.
- `git diff --check`.

## 향후 지침

Text module을 추가하거나 rename할 때는 test job artifact name과 `nightly-tests.yml`의
`Validate coverage artifacts` expected list를 함께 업데이트한다.
