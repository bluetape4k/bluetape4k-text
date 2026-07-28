# README class/ERD 라우팅

## 배경

README class와 ERD 이미지는 문서, 블로그 글, 발표 자료에서 재사용하기 위해
bluetape4k 작업 공간 전반에서 다시 생성됐다.

## 결정

Class와 ERD 다이어그램에는 방해 요소를 고려해 경로를 고르는 직교 connector 라우팅을
사용한다. 파스텔 색상과 기존 글꼴 체계는 유지하되, cubic curve와 component 내부를
가로지르는 connector path는 피한다.

## 결과

다시 생성된 class/ERD SVG는 관계를 고려한 component 배치, 직선 수평/수직 lane,
작은 arrow marker, top/bottom port와 수직 first/final segment를 사용한다. 수평 lane은
component edge가 아니라 row midline 근처에 둔다.

## 검증

- `node --check .omx/scripts/refine-readme-diagrams.mjs`
- 변경된 class/ERD SVG: cubic connector count `0`
- 변경된 class/ERD SVG: card-interior crossing candidate `0`

## 향후 지침

다이어그램을 다시 생성할 때는 방해 요소를 고려한 경로 점수 계산을 보존하고, 대규모
이미지 변경을 수락하기 전에 contact sheet를 검사한다.
