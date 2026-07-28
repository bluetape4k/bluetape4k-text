# 2026-05-20 — README overview 시각 자료 배치

## 배경

README 다이어그램과 차트는 장식용 생성 아티팩트가 아니라 소스 근거가 있는 문서로
다뤄야 한다. 이번 작업은 2026 reference document와 공유 README diagram style guide를
사용했지만, 모듈 이름과 group 기준은 source code와 build layout에 남았다.

## 결정

Root README를 위한 영문 전용 SVG+PNG overview 시각 자료를 추가하고, overview
다이어그램을 설치, 사용법, build instruction보다 앞에 배치한다. 기존 Architecture/Diagram
섹션이 사용 예제 뒤에 붙어 있던 경우 위로 이동한다.

## 결과

`bluetape4k-text`는 이제 root README overview diagram과 module composition chart를 가진다.
README 시각 자료 배치는 overview-first rule을 따른다. 생성된 레이블은 이미지 안에
현지화 문구를 넣지 않는다.

## 검증

- 생성된 SVG 파일을 `xmllint --noout`로 parse했다.
- 생성된 PNG 파일을 `rsvg-convert`로 렌더링했다.
- Workspace README image-link scan은 missing local image 0건을 보고했다.
- Workspace Architecture/Diagram ordering scan은 Installation, Usage, Examples, Build
  heading 뒤에 남은 섹션 0건을 보고했다.
- 생성된 root overview SVG text는 non-ASCII character를 포함하지 않았다.

## 향후 메모

Architecture diagram을 README 파일 끝에 붙이지 않는다. Overview 또는 architecture
diagram은 상단 근처에 두고, class, sequence, ERD, flow diagram은 설명 대상 섹션 옆에
배치한다.

Root overview diagram과 composition chart는 BOM이 있으면 먼저 배치하고, Examples 또는
Additional examples가 있으면 마지막에 배치한다. 중간 group은 repo-specific README가
alphabetic grouping을 요구하지 않는 한 source-backed orientation order를 유지한다.
