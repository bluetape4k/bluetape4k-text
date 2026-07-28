# Code scanning workflow permission

## 배경

GitHub CodeQL이 Nightly, snapshot publish, release workflow에 대해
`actions/missing-workflow-permissions` alert를 보고했다.

## 결정

Checkout을 사용하는 workflow에는 명시적인 workflow-level `contents: read` permission을
선언한다. Token이 필요 없는 job은 `permissions: {}`로 override하고, `contents: write`는
release를 생성하는 GitHub Release job에만 둔다.

## 결과

CI, publish, release 동작을 바꾸지 않으면서 alert 대상 job의 workflow token default가 최소
권한 원칙을 따른다.

## 검증

- `actionlint .github/workflows/nightly-tests.yml .github/workflows/publish-snapshot.yml .github/workflows/release.yml`
- `yq` inspection of workflow and job permissions
- `git diff --check`

## 향후 guard

앞으로 GitHub Actions를 수정할 때는 먼저 명시적인 workflow-level `permissions` block을
추가한다. 그 다음 write access가 필요한 step이 있을 때만 개별 job 권한을 넓힌다.
