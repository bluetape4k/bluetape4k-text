# README 다이어그램 이미지 검증

## 배경

`bluetape4k-text` README 다이어그램은 공유 파스텔 인포그래픽 렌더러로 갱신됐다.
작업 범위에는 현재 Mermaid 블록과 git history에서 복구한 기존 README 다이어그램
이미지 링크가 포함된다.

## 결정

README에 노출되는 아티팩트는 PNG를 사용하고, 재사용을 위해 SVG 원본을 PNG 옆에 둔다.
다이어그램 레이블은 영어만 사용한다. `Diagram`, `Architecture`, `Sequence Diagram` 같은
일반 제목은 모듈별 영어 제목으로 교체한다. 비영어 문구를 잃은 시퀀스 레이블은 의미
없는 일반 레이블 대신 참여 component를 대체 값으로 사용한다.

## 결과

- 렌더링된 아티팩트 20개
- PNG 파일 10개
- SVG 원본 파일 10개
- 누락된 README 이미지 링크 없음
- README 파일의 local SVG 이미지 임베드 없음
- 남은 Mermaid 코드 블록 없음
- Shape-check candidate 없음

## 검증

- `node /Users/debop/work/bluetape4k/.omx/scripts/refine-readme-diagrams.mjs .`
- README 이미지 링크 및 Mermaid residue checker
- PNG/SVG shape checker
- Visual contact sheet review: `/tmp/bluetape4k-text-diagram-review-samples.png`
- `git diff --check`

## 향후 지침

가능하면 git history에서 이전에 교체된 블록까지 포함해 원본 Mermaid source에서 다시
생성한다. 이미지 크기는 내용 기준으로 유지하고, 가짜 filler node를 피하며, SVG 원본을
보존하고, 게시 전 sample sheet를 검사한다.
