# Issue #214 Detekt와 Kotlin 2.4 호환성

## 배경

저장소의 Kotlin 버전이 `2.4.0`으로 올라간 뒤에도 Detekt 플러그인이 `1.23.8`에
고정되어 있어, 기존 root `detekt` task는 실제 Kotlin 모듈을 분석하지 못했다. 플러그인을
모듈에 적용한 첫 실행에서는 `detekt was compiled with Kotlin 2.0.21 but is currently
running with 2.4.0` 오류가 재현되었다.

## 결정

Kotlin `2.4.0`과 호환되는 Detekt `2.0.0-alpha.5` 및 `dev.detekt` 플러그인 ID를 사용한다.
Detekt 2의 API 변경에 맞춰 XML 보고서는 `reports.checkstyle`로 활성화하고, 모듈별
`detekt-baseline.xml`은 기존 위반만 억제하도록 유지한다. `ignoreFailures`는 활성화하지
않아 baseline에 없는 새 위반은 계속 quality gate를 실패시킨다.

## 결과

모든 Kotlin 소스 모듈에 `detekt` task가 등록되고, root `detekt` task가 모듈 task를
집계한 뒤 `build/reports/detekt/merged.xml`을 생성한다. Nightly workflow는 root와 모듈
보고서를 모두 artifact로 보존하며, 보고서가 없으면 실패한다.

## 검증

- `./gradlew detekt --no-daemon --no-configuration-cache`
- 새 `FunctionOnlyReturningConstant` 위반을 임시로 추가한 `./gradlew :tokenizer-core:detekt`가 실패하는지 확인
- `./gradlew test --no-daemon --no-configuration-cache`
- `git diff --check`

## 향후 지침

Kotlin 버전을 올릴 때 Detekt compatibility table을 먼저 확인한다. Detekt 2가 아직
alpha 단계인 동안에는 플러그인 API 변경과 Kotlin 분석 엔진 호환성을 함께 검증하고,
baseline을 갱신할 때는 기존 항목만 유지하며 새 위반을 무심코 추가하지 않는다.
