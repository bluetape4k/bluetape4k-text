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
기존 동기 facade는 API 호환성을 유지하되 최초 직접 조회의 blocking 비용을
KDoc에 명시합니다.

## 검증

- `JapaneseDictionaryProviderTest`: 전체 96개 테스트 통과
- `KoreanDictionaryProviderTest`: 전체 181개 테스트 통과
- 동시 초기화 16개 호출에서 initializer 1회, 취소 후 재시도 2회 검증
- 두 provider의 실제 `preload()` snapshot 준비 테스트 통과
- 두 모듈 `build -x test`, `detekt`, `git diff --check` 통과

## 향후 규칙

새 dictionary loader를 추가할 때는 동기 `lazy`·`publicLazy`에 리소스 I/O를
직접 넣지 말고, 성공·실패·취소 lifecycle을 검증하는 suspend preload 경로와
단일 초기화 테스트를 함께 추가합니다. 요청 처리 경로에서 첫 동기 조회가
발생하지 않도록 애플리케이션 startup coroutine에서 provider의 `preload()`를
호출합니다.
