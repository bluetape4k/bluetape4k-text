# Issue 67 matches Flow 스트리밍

## 배경

`matchesAsFlow()`는 협력적 스트리밍을 제공한다고 문서화되어 있었지만,
방출 전에 `parseText(text)`를 호출했다. 그 결과 `take(N)`으로도 전체 매치
생성을 피할 수 없었다.

## 결정

기본 Flow 경로에서는 원시 트라이를 순회하고, 매치를 찾는 즉시 채널로 전송한다.
겹침 제거 또는 단어 경계 필터처럼 전체 결과 후처리가 필요한 옵션은
즉시 실행하는 `parseText` 경로를 유지한다.

## 결과

기본 `matchesAsFlow()`는 더 이상 `parseText`의 전체 결과 생성에 의존하지 않으며,
문서에 명시된 대로 `stopOnFirstMatch`를 무시한다. 회귀 테스트는 동일한
오토마톤에서 `parseText`가 하나의 매치를 반환하는 동안 Flow가 모든 매치를
방출하는지 검증한다.

## 검증

`./gradlew :text-search:test --tests 'io.bluetape4k.text.search.flow.AhoCorasickFlowTest' --no-daemon --no-configuration-cache --no-build-cache`

## 향후 유의 사항

겹침 및 단어 경계 필터를 매치 순서를 바꾸지 않고 증분 적용할 수 있다는 근거가
있을 때까지 즉시 실행 fallback을 제거하지 않는다.
