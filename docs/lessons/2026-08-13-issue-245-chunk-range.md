# Issue #245 ChunkMatch range 경계 계약

## 배경

`KoreanChunker.ChunkMatch`의 `start`와 `end`는 정규식 `Matcher`에서 가져온
offset이다. `Matcher.end()`는 매치 끝 다음 위치를 가리키는 exclusive 값인데,
공개 `range`가 `start..end`로 계산되어 다음 문자를 매치에 포함하는 결함이
발견됐다.

## 근본 원인

청크 분할과 `disjoint`는 이미 `end`를 exclusive로 사용하고 있었지만,
`range`만 Kotlin의 inclusive 범위 연산자인 `..`를 사용했다. 그 결과
`ChunkMatch(0, 2, "ab", ...)`의 `range`가 `0..2`가 되어 `"abX"`에서
`"abX"`를 반환했다.

## 결정

- `ChunkMatch.range`는 `start until end`로 계산해 `end`를 exclusive로 유지한다.
- `start`, `end`, 생성자, `disjoint`의 기존 ABI와 내부 matcher offset 계산은
  변경하지 않는다.
- KDoc에 `Matcher.end()`와 `start until end` 계약을 명시하고,
  `substring` 경계와 맞닿은 청크의 `disjoint` 동작을 회귀 테스트로 고정한다.

## 검증

- 수정 전 회귀 테스트: `range`가 `0..2`를 반환해 RED 확인
- 수정 후 `KoreanChunkerTest`: 10건 PASS
- `:tokenizer-korean:test`: 185건 PASS, 1건 skipped
- `detekt`: `BUILD SUCCESSFUL`
- `build -x test`: `BUILD SUCCESSFUL`
- `git diff --check`: 통과

## 후속 지침

새로운 offset 또는 `IntRange` API를 추가할 때는 시작 위치 포함 여부와 끝 위치
포함 여부를 KDoc에 명시하고, `substring` 또는 `slice` 경계 및 인접 구간
테스트를 함께 작성한다.

## 관련 항목

- Issue #245
