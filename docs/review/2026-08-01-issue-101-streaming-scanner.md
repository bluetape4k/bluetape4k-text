# Aho-Corasick streaming scanner 검토

## 상태 모델

`AhoCorasickAutomaton.scanner()`는 `AhoCorasickScanner`를 생성한다. `scan(chunk)`은 직전 꼬리와 새 청크를 함께 검색하고, 키워드 최대 길이를 고려해 끝 위치가 확정된 결과만 방출한다. `finish()`는 남은 꼬리를 비우고, `reset()`은 offset과 중복 방지 상태를 새 입력 스트림 기준으로 초기화한다.

## 경계 및 호환성

- 결과 offset은 청크를 이어 붙인 가상 입력의 UTF-16 인덱스이며 기존 `AhoCorasickMatch`의 inclusive `end`를 유지한다.
- `ignoreCase`, overlap, word-boundary 설정은 기존 `parseText` 경로를 재사용한다.
- 정규화는 청크 사이의 원문 offset 역매핑을 보장할 수 없어 `NormalizationForm.NONE`만 허용하고, 다른 값은 생성 시 명시적으로 거부한다.
- deferred match의 재평가로 생기는 중복은 최근 꼬리 범위에서만 추적하고, 꼬리 밖으로 밀려난 키는 제거한다.

## 메모리 계약

전체 입력을 누적하지 않는다. 보관하는 문자열은 키워드 최대 길이의 두 배와 경계 문자 하나로 제한하며, dense input에서도 이미 방출한 결과를 다시 반환하지 않는다.

## 검증

키워드가 여러 청크에 걸치는 경우, 조기 방출과 중복 방지, ignore-case·공백 경계, 정규화 거부, reset 동작을 테스트했다. 기존 text-search 전체 테스트도 함께 실행한다.
