# Issue #246: DictionaryProvider 취소와 resource lifecycle 계약

## Context

`DictionaryProvider.readWords`와 `readWordsAsSet`은 여러 classpath dictionary를
`Flow.async`로 병렬 로드하고, blocking resource read는 `Dispatchers.IO`에서
수행한다. 기존 구현은 정상 로딩과 누락 resource를 확인했지만, 실제 `Job.cancel()`이
진행 중인 read와 열린 stream까지 정리하는지는 검증하지 않았다.

## Root Cause

`Dispatchers.IO`는 blocking read를 실행할 dispatcher일 뿐, 해당 blocking 호출을
coroutine cancellation에 자동으로 interruptible하게 만들지는 않는다. 따라서
`readFileByLineFromResources`가 느린 stream을 읽는 동안 호출 coroutine을 취소하면
child가 완료되지 않고 stream close도 지연될 수 있었다.

## Decision

두 Flow 기반 API의 각 resource read를 `runInterruptible(Dispatchers.IO)`로 감싼다.
기존 `withContext(Dispatchers.IO)`와 `Flow.async` 병렬·순서 semantics는 유지하고,
취소 시 blocking read thread를 interrupt해 기존 `use {}` cleanup이 실행되도록 한다.

## Verification

- 새 custom `ClassLoader`와 blocking `InputStream`으로 실제 `Job.cancel()` 경로를
  재현했다.
- 수정 전 cancellation regression은 5초 안에 stream close/child completion을
  만들지 못해 RED였다.
- 수정 후 `./gradlew :tokenizer-core:test --tests
  "io.bluetape4k.tokenizer.utils.DictionaryProviderTest" --no-build-cache`에서
  8개 테스트가 통과했다.
- 정상 다중 resource 병합, stream close, 누락 resource, child failure 전파를
  함께 확인했다.

## Surprise / Miss

`withContext(Dispatchers.IO)`만으로는 resource lifecycle의 cancellation 계약을
증명할 수 없었다. 단순히 `Job.isCancelled`만 확인하면 blocking child와 stream이
남아 있는 상태를 놓칠 수 있으므로, 테스트는 실제 blocking read 시작 신호와 stream
close, child completion을 각각 관찰해야 한다.

## Future Guard

새로운 blocking I/O를 coroutine Flow 안에 추가할 때는 dispatcher 선택과
cancellation interruptibility를 별도 계약으로 검토한다. 취소 테스트는 수동으로
`CancellationException`을 던지지 말고 실제 `Job.cancel()`을 사용하며, resource
`close`와 child completion을 함께 검증한다.
