# Release workflow 표준화

## 배경

Central Portal release campaign은 `bluetape4k-projects`를 canonical release workflow
형태로 사용한다.

## 결정

Release-prep workflow 파일 이름을 `nightly-tests.yml`과 `publish-snapshot.yml`로 바꾸되,
workflow display name은 유지한다.

## 결과

Release preparation script는 bluetape4k 저장소 전반에서 같은 workflow 파일 이름에
의존할 수 있다.

## 검증

- `actionlint .github/workflows/nightly-tests.yml .github/workflows/publish-snapshot.yml .github/workflows/release.yml`

## 향후 guard

Repo-specific exception이 `AGENTS.md`에 문서화되어 있지 않다면 release workflow 파일
이름을 `bluetape4k-projects`와 맞춘 상태로 유지한다.
