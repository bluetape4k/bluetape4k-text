# Issue #332: eager와 streaming 검색 결과 순서

## 배경

`parseText()`의 공개 KDoc은 시작 위치 오름차순을 약속했지만, Aho-Corasick
failure emit 순서를 그대로 매핑해 suffix match가 뒤섞일 수 있었다.

## 결정과 발견

eager `parseText()`는 결과를 `start ASC`, 동일 시작 위치에서 `length DESC`,
동률 시 `keyword ASC`로 정렬한다. `stopOnFirstMatch`는 이 정렬 전에 첫 raw
match에서 탐색을 중단한다. 기본 `matchesAsFlow()`와 `AhoCorasickScanner`는
전체 결과를 정렬하지 않고 raw traversal 순서를 유지한다.

## 결과

`a`/`ba` suffix와 `a`/`ab` 동일 시작 overlap의 계약을 회귀 테스트로 고정하고,
eager 정렬 비용을 관찰할 `parseTextOrderedMatches` benchmark 시나리오를
기존 harness에 추가했다.

## 검증

- 수정 전 RED: `AhoCorasickAutomatonTest` 14개 중 1개 실패, `parseText("ba")`의
  실제 순서는 `[a, ba]`였다.
- 최종 GREEN: `./gradlew :text-search:test :text-search:detekt --no-parallel --no-configuration-cache --console=plain`, 109개 테스트와 정적 검사 통과.
- `./gradlew :text-search:ahocorasickBenchmark --no-parallel --no-configuration-cache --console=plain`: 기존 harness 8개 시나리오 통과. 새 20,000개 매치 정렬 시나리오는 로컬 평균 1,920.577 ops/s였으며 짧은 측정의 신뢰 구간이 넓어 운영 처리량 보장으로 사용하지 않는다.
- 독립 설계 리뷰에서 `firstMatch()`가 정렬 뒤 최소값을 다시 찾는 중복 연산을 발견했다. 이 경로에는 `sortMatches=false`를 전달해 기존 선형 선택을 유지하고 테스트를 다시 실행했다.

## 이후 작업 지침

eager 결과 정렬을 Flow와 scanner에 공통 적용하면 조기 방출과 `take(N)` 취소
계약을 약화시킬 수 있다. 새 후처리 경로는 materialize 필요 여부와 raw 순서
계약을 별도로 문서화하고 테스트한다.
