# Issue #239 bounded dictionary snapshot lesson

## 배경

`VersionedDictionary`가 성공한 모든 이전 snapshot을 보관해 rollback journal이
무제한으로 커지고, 한국어 provider의 반복 mutation이 대형 dictionary snapshot을
계속 누적시키는 문제가 있었다. Issue #240의 mutable read path 원자성은 이번 변경의
범위에서 제외했다.

## 결정

- 공개 `VersionedDictionary`의 `historyCapacity` 기본값은 `1`로 두어 기존
  one-step rollback을 유지하고, `0`은 journal을 비활성화한다. 음수는 즉시
  `IllegalArgumentException`으로 거부한다.
- 기존 `VersionedDictionary(DictionarySnapshot)` JVM 생성자는 `@JvmOverloads`로
  유지하고, 새 capacity 생성자를 추가했다. capacity 초과 snapshot은 FIFO로 제거한다.
- 한국어·일본어 provider는 rollback API를 공개하지 않으므로 내부 history capacity를
  `0`으로 설정한다.
- 한국어 provider mutation publish는 immutable snapshot map과 미변경 entry를
  재사용하고, 실제 변경된 품사 또는 severity fan-out entry만 새 `Set`으로 만든다.
  semantic no-op은 revision만 증가시키고 map/entry identity를 재사용한다.
- public mutable `CharArraySet` 직접 변경은 기존 호환성을 위해 남기되 versioned
  snapshot 계약에는 포함하지 않는다. revision이 필요한 호출자는 provider mutation
  API를 사용해야 한다.

## 검증

- `VersionedDictionaryTest`: 9 passing. 음수/0/기본/유한 capacity, FIFO eviction,
  실패한 reload의 journal 보존, 동시 snapshot 조회를 검증했다.
- 영향 모듈 테스트: `:tokenizer-core:test`, `:tokenizer-korean:test`,
  `:tokenizer-japanese:test`, `:text-search:test`가 모두 `BUILD SUCCESSFUL`이다.
  Korean provider mutation-heavy 테스트는 `@ResourceLock`으로 전역 provider를
  직렬화했다.
- `:tokenizer-core:detekt`, `:tokenizer-korean:detekt`,
  `:tokenizer-japanese:detekt`, `:text-search:detekt`가 통과했다.
- `javap`로 다음 JVM 생성자를 확인했다.
  - `VersionedDictionary(DictionarySnapshot)`
  - `VersionedDictionary(DictionarySnapshot, int)`

## benchmark 기준선

실행 명령은 `./gradlew :text-search:dictionaryBenchmark`이며, raw 결과는
[`docs/benchmark/2026-08-11-issue-239-versioned-dictionary-baselines.json`](../benchmark/2026-08-11-issue-239-versioned-dictionary-baselines.json)에 보존했다.
Apple M4 Pro, Oracle GraalVM JDK 25.0.4, JMH thread 1개/fork 1개에서 측정한 결과는
다음과 같다.

| 경로 | Ops/s | 99.9% error |
|---|---:|---:|
| production COW add/remove | 63.85 | ±29.30 |
| production full replacement | 43.00 | ±36.52 |

신뢰구간이 겹치므로 이 단일 실행을 통계적으로 유의한 처리량 개선으로 해석하지
않는다. Gradle JSON에 `gc.alloc.rate.norm` secondary metric이 없어 allocation이나
heap-retention 개선도 주장하지 않는다. retention 상한의 완료 근거는 결정적 bounded
history 회귀 테스트다.

## 후속 작업

- Issue #240에서 provider의 public mutable collection read path를 immutable atomic
  snapshot read path로 교체하고 concurrent reload/read를 별도로 검증한다.
- 여러 단계 rollback이 필요한 consumer는 `historyCapacity > 1`을 명시적으로
  선택해야 한다. 기본값 변경에 의존하지 않도록 migration 문서를 유지한다.
