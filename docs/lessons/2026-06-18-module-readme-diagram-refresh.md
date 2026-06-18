# Module README Diagram Refresh

## Context

`bom`, `lingua`, `text-search`, `tokenizer-core`, `tokenizer-japanese`, `tokenizer-korean`
README 다이어그램을 모듈 단위로 다시 그렸다. 기존 산출물은 Mermaid/Graphviz 변환 흔적이
남아 있거나, 원본 source 계약보다 클래스 덤프에 가까워 README 독자가 읽기 어려웠다.

반복 문제는 다음과 같았다.

- class diagram에서 상속선과 dependency 선이 카드 내부 텍스트를 지나갔다.
- sequence/flow diagram에서 line label이 선 위에 놓이거나, 사선과 직각선이 섞였다.
- tokenizer 계열은 모든 클래스를 한 장에 나열하면 선이 지저분해져 핵심 계약이 흐려졌다.

## Decision

모듈 README 다이어그램은 클래스 목록보다 독자 질문을 먼저 기준으로 삼는다.

- 클래스 다이어그램은 public facade, request/response model, dictionary/runtime state처럼
  source-backed contract 묶음으로 나눈다.
- 관계선이 3개 이상 같은 카드에서 나가면 개별 카드 대신 group/layer boundary로 의미를
  묶을 수 있는지 먼저 검토한다.
- 선을 삭제하기 전에 카드 위치, 포트, 열린 corridor를 바꿔 본다.
- PNG 렌더 후에는 dense cluster마다 source card exit, target entry, sibling arrows,
  text overlap 순서로 확인한다.

## Outcome

모든 README 참조 이미지를 `docs/images/readme-diagrams` 아래 SVG/PNG 쌍으로 유지했다.

- root README diagrams
- `bom` README diagram
- `lingua` architecture diagram
- `text-search` class, sequence, processing-flow diagrams
- `tokenizer-core` class contract diagram
- `tokenizer-japanese` class contract diagram
- `tokenizer-korean` class contract diagram

## Verification

- `xmllint --noout` for changed SVG files
- CairoSVG render with `~/.local/bin/cairosvg ... -s 2`
- connector audit: diagonal connector count and marker color mismatch count
- README image link checks for localized README files
- rendered PNG visual inspection, including contact sheet for `text-search`
- `git diff --check`

## Future Guidance

Tokenizer diagrams should stay contract-oriented.

- `tokenizer-core`: message models, options, dictionary utilities, exception hierarchy.
- `tokenizer-japanese`: facade delegation, Kuromoji tokenizer, POS helpers, blockword dictionary.
- `tokenizer-korean`: processor responsibility groups and dictionary ownership, not every helper class.

When a dependency line crosses a card, first move the line to a top/bottom corridor. If that creates a
long detour, move the card or connect to a group boundary instead of accepting the crossing.
