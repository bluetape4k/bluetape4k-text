# Issue #148 CodeQL Kotlin 2.4 검토

날짜: 2026-06-26
범위: `.github/workflows/code-quality.yml`
이슈: #148

## 검토 결과

- P0: 0
- P1: 0
- P2/P3: 없음

## 증거

- Code Quality Analysis run `28214748358`은 `Analyze (java-kotlin)`에서만 실패했고 `Analyze (actions)`는 통과했다.
- 같은 `develop` head에서 Nightly, CI, Publish Snapshot run은 모두 green이었으므로 이는 product regression이 아니라 CodeQL extractor support-window 지연이다.
- Workflow matrix는 `actions`를 계속 활성화하고, CodeQL이 Kotlin 2.4.x를 지원할 때까지 `java-kotlin`만 일시 중지한다.
- 휴면 상태의 `java-kotlin` build command는 `assemble`이므로 향후 다시 활성화해도 compile-only 상태를 유지한다.

## 검증

- `actionlint .github/workflows/code-quality.yml`: PASS
- `./gradlew assemble --no-daemon`: PASS
- `git diff --check`: PASS

## Gate 판정

PASS. 변경은 의도한 CodeQL workflow 표면으로 제한되며, GitHub Actions scanning을 보존하고, workflow comment에 재활성화 조건을 기록한다.
