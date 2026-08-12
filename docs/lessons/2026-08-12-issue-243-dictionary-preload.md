# 사전 provider의 suspend preload와 단일 초기화

## 맥락

한국어·일본어 dictionary provider의 동기 facade는 최초 호출에서 리소스 I/O를
수행했고, 한국어의 `publicLazy`는 동시 최초 접근에서 initializer를 중복 실행할
수 있었습니다. 이 경로는 event-loop 호출자를 차단하고 같은 리소스를 읽은 뒤
버리는 비용을 만들 수 있습니다.

## 원인

`LazyThreadSafetyMode.PUBLICATION`은 먼저 publish된 값을 공유하지만 initializer의
중복 실행을 막지 않습니다. 기존 loader는 동기 프로퍼티 안에서
`runBlocking(Dispatchers.IO)`를 사용했기 때문에 suspend 호출자가 명시적으로
초기화를 시작할 방법도 없었습니다.

## 결정

각 provider는 `Mutex`로 보호되는 suspend memoizer를 사용합니다. 성공한 값만
캐시하고 실패·취소 시 상태를 미초기화로 유지하여 다음 호출이 재시도할 수 있게
합니다. `preload()`는 같은 loader를 호출하므로 동시 warm-up도 한 번만 실행하며,
공식 `KoreanProcessor`/`JapaneseProcessor` facade가 이 API를 위임합니다. 기존
동기 facade는 API 호환성을 유지하되 최초 직접 조회의 blocking 비용을 KDoc와
README에 명시합니다.

## 검증

- 두 provider 테스트: 동시 초기화 16개 호출에서 initializer 1회 검증
- 실제 `Job.cancel()`과 직접 `CancellationException` 후 재시도 2회 검증
- 두 provider의 실제 `preload()` snapshot 준비 테스트 통과
- 두 processor facade에서 `preload()` 위임과 snapshot 준비를 검증
- cold/warm timing 관측: 일본어 `59ms/0ms`, 한국어 `382ms/1ms`
  (각 provider timing 테스트를 단독 Gradle invocation으로 실행한 fresh JVM)
- 재현 명령:
  `./gradlew :tokenizer-japanese:test --tests
  'io.bluetape4k.tokenizer.japanese.utils.JapaneseDictionaryProviderTest.preload cold warm timing을 기록한다'
  --no-build-cache --console=plain --info`
  및
  `./gradlew :tokenizer-korean:test --tests
  'io.bluetape4k.tokenizer.korean.utils.KoreanDictionaryProviderTest.preload cold warm timing을 기록한다'
  --no-build-cache --console=plain`
- timing assertion은 cold/warm 각각의 비음수 결과만 보장하는 관측용이며
  deterministic benchmark 또는 성능 회귀 임계값으로 해석하지 않습니다.
- 두 모듈 전체 테스트, `build -x test`, `detekt`, `git diff --check` 통과

## 향후 규칙

새 dictionary loader를 추가할 때는 동기 `lazy`·`publicLazy`에 리소스 I/O를
직접 넣지 말고, 성공·실패·취소 lifecycle을 검증하는 suspend preload 경로와
단일 초기화 테스트를 함께 추가합니다. 요청 처리 경로에서 첫 동기 조회가
발생하지 않도록 애플리케이션 startup coroutine에서 공개 facade의 `preload()`를
호출합니다. cold/warm latency를 운영 readiness 지표로 기록하되 사용자 원문은
관측 label에 넣지 않습니다.
