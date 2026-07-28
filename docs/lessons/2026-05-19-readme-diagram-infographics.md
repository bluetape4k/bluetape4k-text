# README 다이어그램 인포그래픽

## 배경

README 파일은 아키텍처, 클래스, 시퀀스, ERD 같은 여러 다이어그램에 Mermaid 코드
블록을 사용하고 있었다. 작업 공간 전체의 시각 기준은 검토된 파스텔 인포그래픽
PNG를 사용하고, 재사용을 위해 SVG 원본 아티팩트를 함께 보관하는 방식으로 바뀌었다.

## 결정

README의 Mermaid 블록을 생성된 PNG 이미지 링크로 교체하고, 대응하는 SVG 원본을 PNG
옆에 저장한다. 다이어그램 안의 문구는 영어만 사용하고, 큰 레이블에는 Architects
Daughter, 세부 문구에는 Comic Mono를 사용한다. 아키텍처, 클래스, 시퀀스, ERD
다이어그램에는 각각에 맞는 배치를 적용한다.

## 결과

README 다이어그램은 `bluetape4k.github.io/docs/readme-diagram-samples`의 공유
2026-05-19 스타일 가이드에 맞춰 렌더링된다. Root README 아티팩트는 존재하는 경우
저장소 로컬 아티팩트 배치 규칙을 따른다.

## 검증

저장소 간 변환 패스에서 `rsvg-convert`로 PNG/SVG 아티팩트를 생성하고 README 링크를
확인했다.

## 향후 지침

README 다이어그램은 편집 가능한 SVG 원본과 함께 PNG 임베드로 유지한다. 시각적
일관성이 중요할 때 원본 Mermaid나 단순 Mermaid 테마 색상 변경으로 되돌리지 않는다.
