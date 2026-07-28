# README 다이어그램 배치 수정

## 배경

후속 시각 QA에서 생성된 README 다이어그램의 배치 결함 두 가지가 발견됐다.

- 일부 architecture connector가 매우 짧은 line segment로 렌더링되어 arrow head만 보였다.
- Sequence participant header label이 header box 위쪽으로 치우쳐 있었다.

관련 sequence 문제도 함께 수정했다. Self-call은 이전에 zero-length arrow로 렌더링되어
독립된 arrow head처럼 보였다.

## 결정

기존 다이어그램 스타일은 유지하고 생성된 SVG/PNG 아티팩트의 geometry만 업데이트한다.
Architecture connector line segment는 인접 card 사이의 보이는 gap을 가로질러야 한다.
Sequence participant label은 architecture card와 같은 vertical-centering baseline을
사용해야 한다. Sequence self-call은 zero-length line 대신 작은 loop로 렌더링한다.

## 검증

- README image link check: missing=0, localSvgImageLinks=0, mermaidResidue=0
- PNG/SVG shape check: shapeCandidates=0
- Architecture short connector check: shortArch=0
- Sequence header alignment check: seqTop=0
- Sequence zero-length arrow check: zeroSeq=0
- `git diff --check`
- 노출된 root architecture와 대표 sequence diagram에 대해 visual sample을 검토했다.

## 향후 지침

Arrow head-only connector는 SVG가 문법적으로 유효해도 렌더링 실패로 취급한다.
PR 생성 전 geometry check는 architecture connector length, sequence header baseline,
sequence self-call arrow를 포함해야 한다.
