# 릴리스 Changelog 섹션

## 배경

0.1.0 release preflight에서 `CHANGELOG.md`의 release note가 여전히 `Unreleased`
아래에 있어 release workflow가 일반 GitHub release note로 fallback할 수 있음을
확인했다.

## 결정

Tag를 만들기 전에 준비된 note를 `## [0.1.0] - 2026-05-17` 아래로 이동한다.

## 결과

Release workflow는 이제 GitHub Release 생성에 사용할 version-specific note를 추출할 수 있다.

## 검증

- `CHANGELOG.md`에서 `0.1.0` 섹션을 확인했다.

## 향후 지침

Release tag를 만들기 전에 `CHANGELOG.md`에 `baseVersion`과 일치하는 섹션이 있는지
검증한다.
