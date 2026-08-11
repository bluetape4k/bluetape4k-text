# Issue #239 bounded dictionary snapshot 구현 계획

## 목적

Issue #239의 snapshot retention 상한과 반복 mutation 비용을 해결한다. 기존
`VersionedDictionary(initial)` 호출과 one-step `rollback()` 계약은 유지하고,
한국어 provider의 변경 entry만 복사하는 publish 경로와 재현 가능한 benchmark를
추가한다. Issue #240의 mutable read path 원자성은 변경하지 않는다.

## 구현 순서

### 1. `VersionedDictionary` history journal 상한

대상: `tokenizer-core/src/main/kotlin/io/bluetape4k/tokenizer/utils/VersionedDictionary.kt`,
`tokenizer-core/src/test/kotlin/io/bluetape4k/tokenizer/utils/VersionedDictionaryTest.kt`

- RED 테스트를 먼저 추가한다.
  - 음수 `historyCapacity`가 `IllegalArgumentException`을 발생시키는지 검증한다.
  - capacity 2에서 네 snapshot으로 reload한 뒤 최신 두 snapshot만 순서대로
    rollback되고 세 번째 rollback은 `IllegalStateException`인지 검증한다.
  - capacity 0에서 reload 후 rollback이 불가능한지 검증한다.
- targeted core test를 실행해 새 생성자 인자가 아직 구현되지 않은 실패를 확인한다.
- `@JvmOverloads` primary constructor로 기존
  `VersionedDictionary(DictionarySnapshot)` JVM descriptor를 유지하면서
  `historyCapacity: Int = 1` two-argument constructor를 추가하고 음수를
  `require`로 거부한다. bytecode에서 두 constructor를 확인한다.
- reload 성공 후에만 이전 snapshot을 journal에 추가하고, 초과분은
  `removeFirst()`로 FIFO eviction한다.
- loader/version/name 검증 실패 시 current snapshot과 journal을 변경하지 않는다.
- `AtomicReference`, `ReentrantLock`, `rollback()` 예외 경계와 공개 API 호환성을
  유지하고 KDoc에 retention/eviction/failure semantics를 기록한다.
- provider 내부 `VersionedDictionary`에는 `historyCapacity = 0`을 명시한다.
  provider rollback API가 없으므로 소비되지 않는 대형 이전 snapshot을 보존하지
  않는다. 공개 core constructor의 기본값 1은 직접 consumer용으로 유지한다.
- 이미 history가 있는 상태에서 loader/name/revision 실패 후 rollback 순서가
  유지되는 테스트와 fixed-thread concurrent reload/snapshot/rollback 테스트를
  추가한다. 기본 capacity 1의 두 번째 rollback 실패도 migration 경계로
  고정한다.

### 2. Korean provider copy-on-write publish

대상: `tokenizer-korean/src/main/kotlin/io/bluetape4k/tokenizer/korean/utils/KoreanDictionaryProvider.kt`,
`tokenizer-korean/src/test/kotlin/io/bluetape4k/tokenizer/korean/utils/KoreanDictionaryProviderTest.kt`

- RED 테스트에서 품사 단어 mutation과 blockword severity mutation의 변경 entry가
  새 set을 갖고, 변경하지 않은 entry set은 이전 snapshot과 동일한 instance를
  공유하는지 검증한다. 테스트는 고유 단어를 사용하고 `finally`에서 원래 상태를
  복원한다.
- mutation helper가 실제 변경 여부와 변경된 `KoreanPos` 또는 fan-out된
  `Severity` 집합을 전달하도록 조정한다. `LOW→{LOW,MIDDLE,HIGH}` 및
  `MIDDLE→{MIDDLE,HIGH}`를 명시적으로 보존한다.
- 현재 immutable snapshot map을 재사용하고 변경 entry만 새 `Set<String>`으로
  만든다. 전체 reload는 기존 full replacement 경로를 유지한다.
- 없는 품사, 중복 add, 없는 remove, 빈 clear는 revision 증가를 보존하되 현재
  map과 entry set을 재사용하는 semantic no-op으로 테스트한다.
- public mutable compatibility map/set과 reload 순서, revision 증가 계약은
  유지하고 `!!` 대신 명시적 null 검증을 사용한다.
- `KoreanProcessor.removeBlockwords`의 noun removal도 provider helper를 통해
  publish하도록 바꾸고, `properNouns`는 versioned snapshot 대상이 아님을
  KDoc/테스트에 명시한다. 기존 무제한 multi-step rollback 축소는 migration
  안내와 기본값 테스트로 고정한다.
- public mutable collection 직접 write는 versioned snapshot contract 밖임을
  KDoc과 회귀 테스트로 고정한다. provider mutation 테스트는
  `@ResourceLock`, 고유 단어, `finally` 복원을 사용한다.
- #240에 해당하는 read path atomicity나 severity 의미 변경은 포함하지 않는다.

### 3. Japanese provider 회귀 보호

대상: `tokenizer-japanese/src/test/kotlin/io/bluetape4k/tokenizer/japanese/utils/JapaneseDictionaryProviderTest.kt`

- 고유 blockword를 반복 add/remove하여 revision이 증가하고 visibility가
  전환되는지 검증한다.
- 테스트 종료 시 `finally`에서 원래 dictionary snapshot을 reload하여 전역 상태를
  복원한다. 단일 set provider의 snapshot copy 경계는 변경하지 않는다.

### 4. 반복 mutation benchmark

대상: `text-search/build.gradle.kts`,
`text-search/src/benchmark/kotlin/io/bluetape4k/text/search/benchmark/VersionedDictionaryBenchmark.kt`,
`docs/benchmark/2026-08-11-issue-239-versioned-dictionary-baselines.json`

- 기존 `text-search` kotlinx-benchmark harness에 `tokenizer-korean`
  benchmark dependency와 `dictionary` configuration을 추가한다.
- 실제 `KoreanDictionaryProvider` add/remove COW pair와
  `reloadDictionaries` full-replacement pair를 Noun-sized multi-entry fixture로
  비교한다. `Scope.Thread`, threads=1, `Setup(Level.Iteration)`, A→B→A 왕복,
  `@OperationsPerInvocation(2)`로 cardinality와 throughput을 고정한다.
- `:text-search:dictionaryBenchmark`를 실행하고 throughput 및 가능한
  `gc.alloc.rate.norm` raw 결과를 지정 문서로 보존한다. 결과는 로컬 baseline이며
  운영 성능 순위가 아님을 명시한다. fixture reset, threads=1, JDK/host
  metadata와 benchmark/ops 필드를 확인하고 profiler metric 미지원 gap을
  문서화한다.
- primary throughput이 error 범위 밖에서 개선되고 secondary allocation이
  악화되지 않을 때만 성능 주장을 유지한다. 상충 결과는 trade-off로 기록하고,
  allocation metric 미지원 시 allocation/heap 개선 주장을 제거한다.
- benchmark task 목록/compile/실행을 순차적으로 검증한다.

### 5. 문서·검증·lesson

대상: `text-search/README.md`, `text-search/README.ko.md`,
`docs/lessons/2026-08-11-issue-239-versioned-dictionary-retention.md`

- README English/Korean benchmark section을 동일한 task, metric, caveat로
  갱신한다.
- core, Korean, Japanese, text-search 테스트와 detekt/compile을 실행한다.
- Kotlin pattern checklist KT-FIN-01..11, KT-TEST-01/02/05, KT-MOD-02/03를
  evidence와 함께 확인하고 적용 불필요 항목은 N/A 근거를 기록한다.
- `git diff --check`와 변경 범위 검토 후 Korean lesson을 작성한다.

### 6. PR 전달

- Issue #239의 live milestone/assignee/labels와 target `develop`을 재확인한다.
- feature branch를 push하고 Korean PR 본문에 `Closes #239`와 `## DoD Status`를
  포함한다.
- exact head, required checks, review threads, mergeability를 재확인한 뒤
  merge-ready 보고에서 멈춘다. merge는 별도 fresh approval 없이는 수행하지 않는다.

## 검증 명령

```bash
./gradlew :tokenizer-core:test :tokenizer-korean:test :tokenizer-japanese:test :text-search:test --no-daemon --no-configuration-cache
./gradlew detekt --no-daemon --no-configuration-cache
./gradlew :text-search:compileBenchmarkKotlin --no-daemon --no-configuration-cache
./gradlew :text-search:dictionaryBenchmark --no-daemon --no-configuration-cache
git diff --check
```

## 위험과 대응

- bounded history의 기본값을 0으로 두면 기존 rollback 사용성이 깨지므로 1을
  기본값으로 고정한다.
- COW에서 public mutable collection을 snapshot에 alias하면 과거 snapshot이
  오염되므로 immutable set 생성과 identity 회귀 테스트를 함께 둔다.
- full reload와 #240 read-state 경계를 섞지 않고, 변경 파일과 테스트를 해당
  범위로 제한한다.

## 완료 정의

- 모든 RED 테스트가 구현 후 통과한다.
- benchmark raw JSON과 bilingual README가 같은 실행 계약을 설명한다.
- fresh verification evidence와 lesson이 존재한다.
- PR은 exact head와 green required checks를 가진 merge-ready 상태이며, 별도
  승인 전에는 merge하지 않는다.

Constraint: Issue #239 범위와 `$bluetape-kotlin-patterns`의 Kotlin API·동시성·테스트 계약을 함께 만족해야 한다.
Rejected: history capacity 기본값 0과 persistent/delta 전면 전환 | 기존 rollback 호환성과 #240 경계가 깨진다.
Confidence: high
Scope-risk: broad
Directive: #240의 mutable read path atomicity는 후속 이슈에서 별도로 검증한다.
Tested: affected module tests, detekt, constructor bytecode, dictionary benchmark, raw JSON, and git diff check passed after implementation.
Not-tested: final GitHub CI/review status remains pending until the PR is created; merge is intentionally outside this execution.
