## 배경

Nightly와 CI matrix job은 Central snapshot에서 upstream `1.11.0-SNAPSHOT` artifact를
resolve하는 동안 간헐적으로 실패했다. Local Central metadata check는 HTTP 200을
반환했지만, GitHub-hosted runner는 간헐적으로 HTTP 403을 받았다.

## 결정

CI와 Nightly Gradle step에 같은 retry 기준을 적용한다. 최대 5회 시도하고 각 시도 사이에
30초를 기다린다.

## 결과

Workflow는 module test를 실패로 표시하기 전에 일시적인 Central snapshot metadata failure가
회복할 시간을 더 준다.

## 검증

- `git diff --check`
- `actionlint .github/workflows/*.yml`

## 향후 지침

Downstream bluetape4k repo가 release되지 않은 upstream snapshot을 소비할 때는 upstream을
먼저 안정화한다. 그 다음 upstream CI와 Nightly gate가 green인 것을 확인한 뒤 downstream
Nightly를 다시 실행한다.
