# Issue #239 PR 전 검토

## 범위

- `tokenizer-core`: `VersionedDictionary` bounded rollback journal
- `tokenizer-korean`: provider copy-on-write publish와 facade mutation 경계
- `tokenizer-japanese`: provider history retention 기본값과 반복 mutation 회귀
- `text-search`: production dictionary benchmark wiring과 bilingual README

## `$bluetape-kotlin-patterns` 확인

- 불변 snapshot은 `AtomicReference`로 공개하고, mutation은 `ReentrantLock.withLock`
  안에서 순서화했다.
- 성공한 loader만 current/journal을 변경하며, 실패한 loader·version·name 검증은
  기존 상태를 보존한다.
- public API에는 Kotlin 기본값을 추가하되 `@JvmOverloads`로 기존 JVM constructor
  descriptor를 보존했다.
- 한국어 provider의 변경 entry만 새 immutable `Set`을 만들고 미변경 entry를
  참조 재사용한다. LOW/MIDDLE/HIGH fan-out은 모든 영향 entry에 action을 적용한다.
- 전역 provider를 만지는 JUnit 테스트는 `@ResourceLock`으로 직렬화하고, mutation
  테스트는 `finally`에서 원래 snapshot을 복원한다.
- benchmark는 production API를 직접 호출하고 A→B→A fixture reset, `Scope.Thread`,
  `threads=1`, `@OperationsPerInvocation(2)`를 고정했다.

## 판정

- 독립 설계/코드/테스트 review의 최종 Step 2-R 판정: P0/P1 없음, `CLEAR`.
- `git diff --check`: 통과.
- 영향 모듈 테스트와 detekt: 통과.
- JVM constructor ABI 확인: 통과.
- dictionary benchmark와 raw JSON: 생성 및 schema/threads/forks/ops 필드 확인.

## 알려진 제한

- benchmark의 두 primary confidence interval이 겹치며 `gc.alloc.rate.norm`이 없어
  처리량 또는 allocation 개선을 확정하지 않는다.
- provider의 public mutable collection direct write는 호환성 경계로 남아 있으며
  versioned snapshot revision에는 기록되지 않는다.
- mutable read path 원자성은 Issue #240의 별도 범위다.
