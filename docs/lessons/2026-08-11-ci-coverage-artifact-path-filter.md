# Path-filter와 Coverage artifact 검증 조건 정렬

## 배경

PR #254에서 모듈 테스트는 모두 통과했지만 `lingua` 경로가 변경되지 않아
`test-lingua`가 정상적으로 skip된 상태에서 `Coverage Report`가 실패했다.
실행되지 않은 job이 업로드할 수 없는 `coverage-lingua`를 expected artifact에
항상 포함하고 있었기 때문이다.

재현 run: <https://github.com/bluetape4k/bluetape4k-text/actions/runs/31497448932>

## 결정

Coverage Report의 expected artifact 목록을 각 모듈 test job의 path-filter 조건과
동일하게 구성한다.

- path-filter가 `true`인 모듈은 coverage artifact를 반드시 검증한다.
- path-filter가 `false`인 모듈은 실행되지 않으므로 expected 목록에서 제외한다.
- `workflow_dispatch`에서는 모든 모듈 test job이 실행되므로 모든 artifact를 검증한다.
- artifact 디렉터리 누락 또는 Kover XML 누락을 실패로 처리하는 검증은 유지한다.
- Coverage Report 자체도 `workflow_dispatch`에서 실행되도록 조건을 정렬한다.

이렇게 하면 skip된 모듈을 성공으로 위장하지 않으면서, 실행되지 않은 job의
artifact를 요구하는 false failure만 제거할 수 있다.

## 검증

- `actionlint .github/workflows/ci.yml`
- path-filter 조합별 expected 목록 정적 시나리오 검증
- 변경 모듈의 GitHub Actions CI에서 Coverage Report 및 CI Status 확인

## 관련 항목

- Issue #255
- PR #254
