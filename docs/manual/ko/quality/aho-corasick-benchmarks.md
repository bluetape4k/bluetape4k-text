# Aho-Corasick 벤치마크

0.3.0 baseline은 대용량 사전, 조밀한 일치, no-match, Flow 수집, 단순 순차 검색, NFKC 정규화의 JMH throughput을 기록한다. `ops/s`가 높을수록 빠르다.

## 측정 환경

| 항목 | 값 |
|---|---|
| 명령 | `./gradlew :text-search:benchmark` |
| host | Apple M4 Pro, 메모리 48 GiB |
| JVM | GraalVM JDK 21.0.11 |
| mode | throughput, 1 thread, 1 fork |
| warmup | 1초 × 2회 |
| 측정 | 1초 × 5회 |

## Baseline 결과

| 벤치마크 | Ops/s | 측정 내용 |
|---|---:|---|
| `parseTextNoMatch` | 12,209.23 | 5,000개 키워드, 일치 없음 |
| `parseTextDenseMatches` | 3,566.90 | 겹치는 후보가 많은 입력 |
| `parseTextLargeDictionary` | 3,116.99 | 5,000개 키워드와 2,000개 일치 token |
| `matchesAsFlowLargeDictionaryCollect` | 712.62 | 대용량 사전 결과를 Flow로 수집 |
| `naiveContainsSmallDictionary` | 248.39 | 1,000개 키워드를 `String.contains`로 순차 검색 |
| `parseTextNfkcNormalization` | 3.68 | NFKC와 대소문자 무시 정규화 |

NFKC 결과는 정규화를 기본으로 켜지 말고 실제 요구로 선택해야 하는 이유를 보여 준다. 수행하는 작업이 다르므로 일반 no-match 결과와 알고리즘 순위처럼 직접 비교해서는 안 된다.

## 올바르게 해석하기

이 수치는 로컬 비교 snapshot이며 운영 환경 순위가 아니다. benchmark 소스, fixture 크기, 명령, JDK, hardware 등급, fork, warmup, 측정 mode, `ops/s` 방향이 호환될 때만 이후 결과와 비교한다.

작은 사전의 순차 검색은 참고 workload다. 모든 애플리케이션에서 같은 배수로 빨라진다는 뜻이 아니다. automaton 생성 비용과 메모리는 throughput 수치에 포함되지 않으므로 자주 교체한다면 따로 측정한다.

## 다시 실행하기

```bash
./gradlew :text-search:benchmark
```

원본 JSON을 보관하고 score와 error 범위를 함께 보고한다. fixture가 달라졌다면 그 내용을 설명하며 서로 다른 환경의 값을 하나의 추세선으로 합치지 않는다.

즉시 결과가 필요하면 일반 검색 메서드를, 수집과 취소 의미가 필요하면 Flow를 선택한다. Unicode 동등성이 실제 요구일 때만 정규화를 켠다.

[텍스트 검색 모듈](../modules/text-search.md), [검색 예제](../examples/text-search-examples.md), [시작과 메모리](../operations/startup-and-memory.md)로 이어서 읽자.

## 소스 근거

- [원본 baseline JSON](https://github.com/bluetape4k/bluetape4k-text/blob/0.3.0/docs/benchmark/2026-06-04-issue-97-ahocorasick-baselines.json)
- [벤치마크 소스](https://github.com/bluetape4k/bluetape4k-text/blob/0.3.0/text-search/src/benchmark/kotlin/io/bluetape4k/text/search/benchmark/AhoCorasickBenchmark.kt)
