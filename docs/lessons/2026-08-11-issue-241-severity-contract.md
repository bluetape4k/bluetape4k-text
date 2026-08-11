# Issue #241 severity tier와 threshold view 정렬

## 배경

한국어 금칙어 리소스는 `LOW=low+middle+high`, `MIDDLE=middle+high`,
`HIGH=high`인 cumulative threshold view를 공개하고 있었다. 반면 runtime
mutation은 입력 severity와 같거나 높은 key만 갱신해, 같은 단어가 리소스에서
왔는지 runtime에서 추가됐는지에 따라 `LOW`/`MIDDLE`/`HIGH` 조회 결과가 달라졌다.

## 결정

- 공개 snapshot은 cumulative threshold view를 canonical contract로 유지한다.
- reload 입력은 exact-tier 목록과 기존 cumulative view를 모두 받아 canonical view로
  정규화한다. 이를 통해 기존 caller의 전체 snapshot 재로드도 보존한다.
- mutation 직전에 현재 cumulative view를 exact tier로 분해하고, 요청한 source tier만
  변경한 뒤 cumulative view를 다시 만든다.
- 따라서 source tier별 공개 범위는 `LOW -> LOW`, `MIDDLE -> LOW/MIDDLE`,
  `HIGH -> LOW/MIDDLE/HIGH`이다.
- `add`, `remove`, `clear`는 같은 source-tier 방향을 사용하며, semantic no-op은
  기존 snapshot map과 entry를 재사용하고 revision만 증가시킨다.

## 결과

리소스 로드와 runtime mutation이 동일한 threshold 정책을 적용한다. higher-tier
단어를 lower-threshold view에서 유지할 수 있어 `remove`와 `clear`도 tier provenance를
잃지 않는다.

## 검증

- 리소스에 없는 고유 단어 3개로 runtime add와 `containsBlockword` 3×3 matrix 검증
- exact-tier reload 후 cumulative view 정규화 검증
- middle remove 및 low clear 후 각 threshold view 검증
- `:tokenizer-korean:test`: 178 passing, 1 pending, `BUILD SUCCESSFUL`
- `detekt`: `BUILD SUCCESSFUL`
- `git diff --check`: 통과

## 후속 지침

새 severity mutation 또는 reload 경로를 추가할 때는 exact-tier 변경과 cumulative
threshold view 재구성을 분리하지 말고 동일한 정규화 경계를 통과시킨다. threshold
계약을 바꾸려면 resource fixture, runtime matrix, facade masking 테스트를 함께
갱신한다.

## 관련 항목

- Issue #241
