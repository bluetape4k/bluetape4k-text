# Module README 다이어그램 갱신

## 배경

`bom`, `lingua`, `text-search`, `tokenizer-core`, `tokenizer-japanese`, `tokenizer-korean`
README 다이어그램을 모듈 단위로 다시 그렸다. 기존 산출물은 Mermaid/Graphviz 변환 흔적이
남아 있거나, 원본 소스 계약보다 클래스 덤프에 가까워 README 독자가 읽기 어려웠다.

반복 문제는 다음과 같았다.

- class diagram에서 상속선과 dependency 선이 카드 내부 텍스트를 지나갔다.
- sequence/flow diagram에서 line label이 선 위에 놓이거나, 사선과 직각선이 섞였다.
- tokenizer 계열은 모든 클래스를 한 장에 나열하면 선이 지저분해져 핵심 계약이 흐려졌다.

## 결정

모듈 README 다이어그램은 클래스 목록보다 독자 질문을 먼저 기준으로 삼는다.

- 클래스 다이어그램은 public facade, request/response model, dictionary/runtime state처럼
  소스 근거가 있는 계약 묶음으로 나눈다.
- 관계선이 3개 이상 같은 카드에서 나가면 개별 카드 대신 group/layer boundary로 의미를
  묶을 수 있는지 먼저 검토한다.
- 선을 삭제하기 전에 카드 위치, 포트, 열린 corridor를 바꿔 본다.
- PNG 렌더 후에는 dense cluster마다 source card exit, target entry, sibling arrows,
  text overlap 순서로 확인한다.

## 결과

모든 README 참조 이미지를 `docs/images/readme-diagrams` 아래 SVG/PNG 쌍으로 유지했다.

- root README diagram
- `bom` README diagram
- `lingua` architecture diagram
- `text-search` class, sequence, processing-flow diagram
- `tokenizer-core` class contract diagram
- `tokenizer-japanese` class contract diagram
- `tokenizer-korean` class contract diagram

## 검증

- 변경된 SVG 파일에 대한 `xmllint --noout`
- `~/.local/bin/cairosvg ... -s 2`로 CairoSVG 렌더링
- Connector audit: diagonal connector count와 marker color mismatch count
- Localized README 파일의 README image link check
- `text-search` contact sheet를 포함한 rendered PNG visual inspection
- `git diff --check`

## 향후 지침

Tokenizer diagram은 계약 중심 상태를 유지해야 한다.

- `tokenizer-core`: message models, options, dictionary utilities, exception hierarchy.
- `tokenizer-japanese`: facade delegation, Kuromoji tokenizer, POS helpers, blockword dictionary.
- `tokenizer-korean`: processor responsibility groups and dictionary ownership, not every helper class.

Dependency line이 카드를 가로지르면 먼저 선을 top/bottom corridor로 옮긴다. 이때 우회
경로가 너무 길어지면 crossing을 받아들이지 말고 카드를 옮기거나 group boundary에 연결한다.
