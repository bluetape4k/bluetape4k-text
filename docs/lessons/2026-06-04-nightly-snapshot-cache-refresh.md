# Nightly Snapshot Cache Refresh

## 배경

서로 다른 module test job이 `io.github.bluetape4k:*:1.11.0-SNAPSHOT` dependency를
resolve하는 동안, 같은 `develop` SHA에서 Nightly가 실패했다. Cross-repository follow-up도
여러 downstream Nightly workflow를 동시에 manual dispatch했을 때 Central snapshot `403`을
재현했다.

## 결정

복원된 Gradle cache가 오래된 Central snapshot metadata를 재사용하지 않도록 Nightly Gradle
invocation에서 dependency를 refresh한다. Scheduled run이 mutable Central snapshot metadata를
한꺼번에 요청하지 않도록 downstream repository들의 Nightly cron minute을 서로 다르게 둔다.

## 결과

Workflow는 Gradle caching을 유지하되, 각 Nightly build, detekt, test, Kover report call이
Gradle에 dependency metadata 재확인을 요청한다. Scheduled `bluetape4k-text` Nightly 시작
minute은 snapshot을 소비하는 다른 downstream repository와 분리했다.

## 검증

- `actionlint .github/workflows/nightly-tests.yml`
- `git diff --check`
- `./gradlew build -x test --parallel --refresh-dependencies --no-daemon`
- `./gradlew :tokenizer-core:test :tokenizer-japanese:test :tokenizer-korean:test :lingua:test :text-search:test --no-daemon --refresh-dependencies`
- `./gradlew detekt --parallel --refresh-dependencies --no-daemon`
- `./gradlew :tokenizer-japanese:koverXmlReport --no-daemon --refresh-dependencies`

## 향후 지침

Nightly가 Central snapshot을 소비할 때는 복원된 Gradle metadata만 기준으로 삼지 않는다.
Module test failure를 source code 문제로 판단하기 전에 snapshot-sensitive Nightly call에
`--refresh-dependencies`를 추가한다. Snapshot-consuming repository를 모두 같은 cron minute에
두지 말고, scheduled downstream Nightly 시작 minute을 분산한다.
