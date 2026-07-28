# Issue #148 CodeQL Kotlin 2.4

## 배경

저장소를 Kotlin 2.4.0으로 옮긴 뒤 Code Quality Analysis가 `Analyze (java-kotlin)`에서
실패했다. Failure message는 CodeQL Kotlin extractor에서 나왔다. Kotlin 2.4.0이 지원되는
extractor window보다 새 버전이라는 내용이었다. 같은 `develop` head에서는 Nightly, CI,
Publish Snapshot run이 green이었다.

## 결정

CodeQL이 Kotlin 2.4.x를 지원할 때까지 CodeQL `java-kotlin` matrix axis만 일시 중지한다.
Workflow analysis가 계속 실행되도록 `actions` scanning은 활성 상태로 둔다.

## 결과

Code Quality workflow는 더 이상 지원되지 않는 Kotlin extractor path를 schedule하지 않는다.
Dormant Java/Kotlin path는 나중에 다시 활성화할 때를 위해 `assemble`을 사용하므로
compile-only 상태를 유지한다.

## 검증

- `actionlint .github/workflows/code-quality.yml`
- `./gradlew assemble --no-daemon`
- `git diff --check`

## 향후 지침

CodeQL scanner 지연을 맞추려고 Kotlin을 downgrade하지 않는다. CodeQL이 Kotlin 2.4.x 지원을
문서화한 뒤에만 `java-kotlin`을 다시 활성화하고, 다시 활성화한 Gradle command는
compile-only로 유지한다.
