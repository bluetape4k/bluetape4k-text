# Issue 97 Aho-Corasick Benchmark Baselines

## 배경

Issue #97에는 production API를 바꾸지 않으면서 `text-search`의 0.2.1 baseline을 측정할
수 있는 근거가 필요했다.

## 결정

Benchmark는 기존 `text-search/src/benchmark` kotlinx-benchmark harness에 둔다. Scenario는
위험 면적별로 나눈다. 대상은 큰 dictionary, Flow collection, 조밀한 match, match가 없는
input, Unicode normalization, 작은 naive `String.contains` comparison이다.

## 결과

Benchmark는 이제 throughput snapshot 6개를 만들고, raw JMH JSON을 `docs/benchmark/` 아래에
보존한다.

## 검증

- `./gradlew :text-search:compileBenchmarkKotlin :text-search:test`
- `./gradlew :text-search:benchmark`
- `jq -e 'length == 6 and all(.[]; .primaryMetric.scoreUnit == "ops/s" and .mode == "thrpt")' docs/benchmark/2026-06-04-issue-97-ahocorasick-baselines.json`
- `git diff --check`

## 향후 지침

Benchmark delta를 비교할 때는 Gradle `:text-search:benchmark` task를 재사용하고,
`ops/s`가 높을수록 낫다는 점을 문서화한다. Local 짧은 구간 run은 운영 순위가
아니라 비교 가능한 snapshot으로 취급한다.
