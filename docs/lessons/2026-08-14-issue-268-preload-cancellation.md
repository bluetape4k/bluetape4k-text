# Issue #268: KoreanDictionaryProvider preload 취소와 재시도 lifecycle

## Context

`KoreanDictionaryProvider.preload()`은 여러 사전 loader를 `async`로 병렬
초기화한다. `readWords` 계열 loader는 이미 `DictionaryProvider` 내부에서
`runInterruptible(Dispatchers.IO)`를 사용했지만, entity frequency와 typo 사전은
각각 `readWordFreqs`·`readWordMap`을 `withContext(Dispatchers.IO)` 안에서 직접
호출하고 있었다.

## Root Cause

`Dispatchers.IO`는 blocking read를 실행할 dispatcher일 뿐, 해당 read를 호출
coroutine의 취소에 맞춰 interrupt하지 않는다. 따라서 public `preload()`가 직접
사용하는 두 loader에서 실제 `Job.cancel()`이 발생하면 child가 끝나지 않고
resource stream의 `use {}` close도 지연될 수 있었다. #246의 `readWords` 회귀
검증만으로는 이 direct blocking 경계를 덮지 못했다.

## Decision

entity frequency와 typo loader의 blocking 구간을
`runInterruptible(Dispatchers.IO)`로 감싼다. public API, `SuspendMemoized`의
성공 값 공유·실패/취소 후 재시도, `preload()`의 병렬 초기화 semantics는 유지한다.
테스트는 reflection으로 singleton state를 조작하지 않고 internal
`resetForTesting()` seam을 통해 각 lifecycle을 격리한다.

## Verification

- 실제 context `ClassLoader`가 반환하는 blocking `InputStream` fixture로
  frequency(`entity-freq.txt.gz`)와 typo(`typos.txt`) loader를 각각 멈췄다.
- 두 테스트 모두 실제 `Job.cancel()` 후 stream close와 child 종료를 기다리고,
  parent classloader로 public `preload()`를 다시 호출한 뒤 모든 snapshot이
  준비됐는지 확인한다.
- 수정 전 두 테스트는 5초 안에 `stream.closed.await`를 통과하지 못하는
  `TimeoutCancellationException`으로 RED였다.
- 수정 후 두 cancellation/retry 테스트가 통과했다.
- `./gradlew :tokenizer-korean:test --no-build-cache --max-workers=1
  --console=plain`: 190 tests passed.
- `./gradlew :tokenizer-korean:build -x test --no-build-cache --max-workers=1
  --console=plain`, `./gradlew detekt --no-build-cache --max-workers=1
  --console=plain`, `./gradlew build -x test --no-build-cache --max-workers=1
  --console=plain`, `git diff --check`: 모두 성공했다.

## Surprise / Miss

기존 cancellation 수정이 `DictionaryProvider.readWords`와
`readWordsAsSet`의 Flow 경계에만 적용되어 있어, 같은 provider의 direct map·freq
loader는 별도 취소 계약으로 남아 있었다. public facade 테스트가 snapshot 준비
결과만 확인하면 이 차이를 놓치므로, 느린 resource를 실제로 주입하고 stream
close를 관찰해야 한다.

## Future Guard

새 dictionary loader가 `readWordFreqs`, `readWordMap`처럼 suspend가 아닌
blocking API를 직접 호출할 때는 반드시 `runInterruptible(Dispatchers.IO)`를
사용하고, public `preload()`의 실제 `Job.cancel()` 후 close·child completion·
재시도를 함께 검증한다. 임의 `InputStream`의 interruption 협조성은 보장되지
않으므로 문서와 테스트의 계약은 협조적인 resource에 대한 취소 응답성으로
한정한다.
