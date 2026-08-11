# VersionedDictionary snapshot retention 설계

## 목적

Issue #239는 런타임 사전 갱신 때 성공한 모든 이전 전체 snapshot을
`VersionedDictionary`가 보존해 장기 실행 프로세스의 heap retention과 갱신
비용이 mutation 횟수에 비례해 증가하는 문제를 해결한다. 이번 변경은 기존
reload/rollback API의 호환성을 유지하면서 retention 상한을 명시하고, 한국어
provider의 고빈도 mutation에서 변경된 entry만 복사하는 경로를 제공한다.

## 현재 근거

- `tokenizer-core/.../VersionedDictionary.kt`는 무제한 `ArrayDeque`에 이전
  `DictionarySnapshot`을 저장하고 `rollback()` 외의 eviction 경로가 없다.
- `tokenizer-korean/.../KoreanDictionaryProvider.kt`는 단어 또는 금칙어 변경 때
  모든 품사·심각도 set을 다시 `toSet()`으로 복사한다.
- `tokenizer-japanese/.../JapaneseDictionaryProvider.kt`는 단일 blockword set을
  snapshot하므로 provider 내부에 공유할 다중 entry map은 없다.
- 실제 tokenizer read path를 immutable snapshot으로 통합하는 문제는 Issue #240의
  별도 범위다. 이번 변경은 public mutable compatibility API와 read path를 바꾸지
  않는다.
- 기존 `text-search` 모듈에 `kotlinx.benchmark` harness와 JMH task가 이미 있어
  신규 benchmark module을 만들 필요가 없다.

## 선택한 설계

### 1. bounded rollback journal

`VersionedDictionary` 생성자에 `historyCapacity: Int = 1`을 추가한다. 기존
`VersionedDictionary(DictionarySnapshot)` JVM constructor descriptor는
`@JvmOverloads`로 유지하고, 새 two-argument constructor를 추가한다.

- `historyCapacity`는 caller configuration이므로 음수가 아니어야 하며, 음수는
  `IllegalArgumentException`으로 거부한다.
- 기본값 `1`은 현재 one-step `rollback()` 사용성을 보존한다.
- `0`은 이전 snapshot을 보존하지 않는 journal 비활성화 모드다. 이 모드에서
  `rollback()`은 기존의 `IllegalStateException` 경계를 유지한다.
- capacity는 현재 snapshot을 제외한 이전 snapshot journal entry 수다. 기본값
  `1`은 기존 source/binary constructor 호출과 one-step rollback 예제를
  보존하지만, 기존 무제한 multi-step rollback은 의도적으로 축소된다. 두 번째
  rollback이 필요한 caller는 명시적으로 더 큰 capacity를 설정해야 한다.
- reload 성공 직후 이전 snapshot을 journal에 추가하고, capacity를 초과하면
  가장 오래된 snapshot부터 제거한다.
- loader 실패와 version/name 검증 실패는 current snapshot과 journal을 모두
  변경하지 않는다.
- rollback은 가장 최근에 보존된 snapshot을 제거하고 공개한다. eviction으로
  이미 제거된 snapshot은 복구 대상이 아니다.

`AtomicReference`와 `ReentrantLock`의 현재 조합은 유지한다. history 상한은
동시성 모델을 바꾸지 않으며, 값을 mutable하게 복제하는 책임도 새로
`VersionedDictionary` 안으로 끌어들이지 않는다.

### 2. provider copy-on-write publish

한국어 provider의 mutation publish helper에 실제로 변경된 key 집합을 전달한다.

- 품사 사전 mutation은 현재 immutable snapshot map을 재사용하고 변경된
  `KoreanPos`의 set만 새로 만든다.
- severity blockword mutation은 mutation fan-out에 따라 변경된 `Severity` entry
  집합을 계산한다. `LOW`는 `{LOW, MIDDLE, HIGH}`, `MIDDLE`은
  `{MIDDLE, HIGH}`, `HIGH`는 `{HIGH}`를 복사하고 나머지 entry set은 공유한다.
- semantic no-op(없는 품사, 중복 add, 없는 단어 remove, 빈 clear)은 기존
  revision 증가 동작을 보존하되 현재 immutable map/value를 그대로 재사용하며
  entry set을 새로 만들지 않는다.
- 전체 `reloadDictionaries`/`reloadBlockwords`는 caller가 전달한 전체 replacement를
  적용하는 명시적 full-reload 경로로 유지한다.
- 일본어 provider는 단일 `Set<String>` 값이므로 set 자체를 새로 만드는 현재
  경계를 유지하고 bounded history만 적용한다.
- tokenizer가 읽는 public mutable map/set과 reload 순서를 바꾸지 않는다. 이
  원자성 이행은 #240에서 별도로 설계·검증한다.
- `KoreanProcessor.addBlockwords`와 `removeBlockwords`의 noun mutation도
  provider mutation helper를 통과시켜 `currentDictionarySnapshot()`의 revision
  및 value와 일치시킨다. `properNouns`는 versioned dictionary 대상이 아니며
  기존 mutable compatibility 경계를 유지한다.
- `koreanDictionary`, `blockWords`, `properNouns`, 일본어
  `blockWordDictionary`의 public mutable collection에 직접 쓰는 행위는 기존
  compatibility surface로 남기지만 versioned snapshot contract에서는 지원하지
  않는다. snapshot revision/value가 필요하면 provider/facade mutation API를
  사용해야 하며, 이 제한과 직접 mutation 후 다음 COW publish가 외부 변경을
  재수집하지 않는다는 사실을 KDoc과 회귀 테스트에 명시한다.

facade mutation과 versioned snapshot의 경계는 다음과 같다.

| API | versioned 대상 | revision 영향 |
|---|---|---|
| `addNounsToDictionary`/`addWordsToDictionary` | 해당 `KoreanPos` entry | `korean-dictionary` 증가 |
| `removeBlockwords`의 noun 제거 | `Noun` entry | `korean-dictionary` 증가 |
| `addBlockwords`의 blockword 변경 | severity fan-out entry | `korean-blockwords` 증가 |
| `clearBlockwords`/`removeBlockword` | severity fan-out entry | `korean-blockwords` 증가 |
| `properNouns` 직접 변경 | versioned 대상 아님 | snapshot revision 불변 |

한국어와 일본어 provider는 내부 `VersionedDictionary` rollback API를 공개하지
않으므로 생성 시 `historyCapacity = 0`을 명시한다. 공개 core store의 기본값
`1`은 직접 consumer의 one-step rollback을 위한 것이며, provider에는 소비되지
않는 이전 대형 snapshot을 보관하지 않는다.

snapshot map에 공유되는 set은 provider가 만든 read-only `Set<String>`이며,
public mutable `CharArraySet`과 alias를 만들지 않는다. 이 경계를 KDoc과
회귀 테스트로 고정한다.

이 COW 경로의 복잡도는 `O(품사 map key 수 + 실제로 변경된 set의 크기)`이다.
따라서 Noun처럼 약 20만 단어인 단일 entry를 바꾸는 경우에는 set 복사 비용이
남는다. 이번 이슈의 완료 주장은 전체 map의 중복 복사·history retention을 줄이는
것으로 한정하며, Noun set 자체의 delta/overlay 전환은 별도 후속 설계 대상으로
기록한다. benchmark에서 Noun 규모 primary throughput이 error 범위 밖에서
개선되고 기록된 secondary allocation metric이 악화되지 않을 때만 map-level
mutation 비용 개선 주장을 유지한다. 그렇지 않으면 결과와 한계를 문서화하고
retention 상한만 성능 완료 기준으로 남긴다.

### 3. benchmark와 regression proof

기존 `text-search` kotlinx-benchmark source set에
`VersionedDictionaryBenchmark`를 추가하고 `tokenizer-korean` production API를
직접 호출한다. benchmark fixture를 별도 COW 알고리즘으로 재구현하지 않는다.

- `KoreanDictionaryProvider.addWordsToDictionary`의 production COW 경로와
  `reloadDictionaries`의 production full-replacement 경로를 비교한다.
- Noun-sized multi-entry fixture를 고정하고, mutating benchmark는
  `Scope.Thread`, JMH `threads=1`, `Setup(Level.Iteration)`으로 baseline을
  재설정한다. 각 invocation은 production `add`/`remove` pair로 A→B→A를
  왕복해 동일 cardinality를 유지하고 `@OperationsPerInvocation(2)`로
  throughput을 정규화한다. full-replacement baseline에도 같은 pair를
  적용한다.
- 결과는 처리량 `ops/s`와 가능할 때 JMH GC profiler의 `gc.alloc.rate.norm`
  (bytes/op)를 함께 기록한다. profiler가 Gradle JSON에 포함되지 않으면 해당
  gap을 DoD에 명시하고 retention은 결정적 rollback 테스트로 증명한다.
- 로컬 비교 snapshot이지 운영 성능 순위가 아님을 명시한다.
- `VersionedDictionaryTest`는 capacity validation, zero-capacity, eviction,
  loader failure, rollback boundary를 결정적으로 검증한다. 이미 history가
  존재하는 상태에서 loader/name/revision 실패 후 journal rollback 순서가
  변하지 않는지 검증하고, 동시 reload/snapshot/rollback의 lock 경계와
  old-snapshot immutability도 fixed-thread 테스트로 검증한다.
- provider 테스트는 반복 mutation에서 revision 증가, 변경 entry 반영, 변경
  entry의 새 instance, 미변경 entry의 참조 동일성, semantic no-op의 map/set
  재사용을 검증한다. facade add/remove 후 noun snapshot revision/value도
  검증하고, LOW/MIDDLE/HIGH fan-out과 direct mutable write limitation도
  검증한다. 전역 provider를 만지는 테스트는 `@ResourceLock`으로 직렬화하고
  고유 단어와 `finally` 복원을 사용한다.
- 일본어 provider는 반복 add/remove revision/visibility와 원래 snapshot 복원,
  bounded journal semantics는 core test로 증명한다. provider에는 rollback
  API가 없다는 사실을 문서화한다.

## 대안과 선택 이유

| 대안 | 장점 | 거부 이유 |
|---|---|---|
| A. bounded journal + provider copy-on-write | 기존 API를 유지하면서 retention과 반복 map 복사를 함께 줄임 | full reload와 대형 단일 set 복사 비용은 여전히 남음 |
| B. opt-in journal (`capacity=0` 기본) | 기본 heap retention이 가장 작음 | 기존 rollback 계약과 예제의 기본 동작을 깨뜨림 |
| C. persistent/delta snapshot 전면 전환 | 장기적으로 가장 큰 structural sharing 가능성 | public read path·migration·#240과 결합되어 이번 이슈의 안전한 변경 단위를 초과함 |

## 실패 모드와 경계

1. 음수 `historyCapacity`가 허용되면 retention 계약이 불명확해지므로 생성 즉시
   `IllegalArgumentException`으로 실패한다.
2. loader가 예외를 던지면 새 snapshot과 journal entry를 추가하지 않아 마지막
   성공 상태를 보존한다.
3. capacity보다 많은 reload 후 rollback을 시도하면 보존된 범위까지만 성공하고,
   그 이후에는 `IllegalStateException`으로 실패한다.
4. provider mutation 중 변경되지 않은 entry를 source mutable set과 alias하면
   과거 snapshot이 오염될 수 있으므로 변경 entry만 새 immutable set으로 만든다.
   semantic no-op은 이미 immutable snapshot map을 재사용한다.
5. #240의 read path가 여전히 mutable collection을 읽는 사실은 이번 변경의
   known limitation이며, 해당 이슈의 concurrent reload/read regression으로
   추적한다.

## 호환성·문서

- 기존 단일 인자 `VersionedDictionary(initial)` 호출은 source 및 JVM binary
  descriptor를 유지하고 one-step rollback을 유지한다. 기본 capacity 1로 인해
  기존 무제한 multi-step rollback은 유지되지 않으며 migration 문서에 명시한다.
- 새 public constructor parameter와 retention semantics는 한국어 KDoc과
  benchmark 문서에 기록한다.
- 변경된 benchmark 문서는 기존 English/Korean README locale과 metric/caveat를
  동기화한다.
- 신규 module, dependency catalog 변경, diagram asset은 없다.

## 완료 기준

- bounded history와 copy-on-write publish가 core/Korean/Japanese 범위에 적용된다.
- core/provider regression tests가 새 경계를 증명한다.
- bytecode/API 확인에서 one-argument constructor와 two-argument constructor가
  모두 존재함을 검증한다.
- `text-search` dictionary benchmark가 실제 Korean production 경로를 호출하고,
  `dictionaryBenchmark` task, raw JSON 고정 경로, fixture reset, threads=1,
  JDK/host metadata와 `benchmark`, `ops/s` 필드를 검증해 남긴다. GC profiler가
  `gc.alloc.rate.norm`을 제공하지 않으면 그 gap과 대체 rollback 증거를
  문서화한다.
- Noun-sized case의 primary throughput이 JMH error 범위 밖에서 개선되고,
  기록된 secondary allocation metric이 악화되지 않을 때만 성능 개선 주장을
  유지한다. metric이 상충하면 trade-off로 기록하고, allocation metric이
  없으면 allocation/heap 개선 주장을 하지 않는다.
- Kotlin checklist, targeted/full tests, detekt, compile, benchmark,
  `git diff --check`가 fresh evidence로 통과한다.
- Issue #240 범위의 read-state atomicity는 변경하지 않는다.
