# 버전 사전 갱신 검토

## 공통 계약

`DictionaryVersion`은 사전 이름과 음이 아닌 단조 증가 revision을 식별한다. `VersionedDictionary`는 현재 `DictionarySnapshot`을 원자적으로 보관하고, loader가 성공한 뒤에만 새 값을 공개한다. revision이 낮거나 이름이 다르면 교체를 거부하며, 실패한 loader는 현재 snapshot과 history를 변경하지 않는다.

성공한 이전 snapshot은 `rollback()`으로 되돌릴 수 있다. snapshot의 값은 얕은 wrapper이므로 호출자는 내부 collection도 읽기 전용으로 유지해야 한다. 기존 `DictionaryProvider`의 Flow 기반 병렬 리소스 로딩 semantics는 변경하지 않았다.

## 한국어·일본어 적용

- 한국어 품사 사전은 `currentDictionarySnapshot`과 `reloadDictionaries`로 전체 품사 사전을 교체한다.
- 한국어 심각도별 금칙어는 `currentBlockwordSnapshot`, `reloadBlockwords`, `containsBlockword`를 제공한다.
- 일본어 금칙어는 같은 계약의 snapshot/reload/contains API를 제공한다.
- 기존 add/remove/clear API는 유지하고 내부 lock과 자동 revision으로 snapshot 이력을 갱신한다.
- 금칙어 processor는 provider의 동기화된 contains 경로를 사용한다.

## 동시성 및 실패 경계

loader가 새 값을 만드는 동안 기존 snapshot은 계속 읽을 수 있고, loader 예외는 기존 값에 영향을 주지 않는다. provider의 공개 mutable collection을 직접 수정하는 외부 코드는 기존 호환성을 위해 허용되지만 자동 revision 추적 범위 밖이므로, 버전 이력을 보존해야 하는 소비자는 reload API를 사용해야 한다.

## 검증

core의 revision·실패·rollback 테스트와 한국어/일본어 provider의 reload/복구 테스트를 추가했다. 세 모듈의 전체 테스트와 Detekt를 실행하고 `git diff --check`를 통과시킨다.
