# Root README 다이어그램 시각 QA

## 배경

루트 README 다이어그램을 `docs/images/readme-diagrams`로 옮기고 다시 그리는 과정에서
반복적인 시각 QA 누락이 발생했다.

- 선 교차 수만 보고 실제 연결 의미와 독자 시선 흐름을 놓쳤다.
- 한 layer의 여백만 줄이고 인접 layer와 내부 카드 위치를 같이 움직이지 않았다.
- 카드 내부 텍스트 폭, layer 내부 여백, 외곽 프레임 안의 전체 component bbox를 서로 다른 단계에서 따로 검증했다.

## 결정

README 다이어그램은 한 장씩 렌더링하고, 다음 기준을 같은 패스에서 함께 확인한다.

- 소스 근거가 있는 관계를 삭제하지 말고 카드 배치와 포트로 해결한다.
- 선은 수평/수직/직각 꺾은선을 우선하고, 사선은 같은 diagram 안에서 섞지 않는다.
- 카드 내부 텍스트는 줄바꿈으로 해결하고 카드 밖으로 나가지 않게 한다.
- layer label 영역을 제외한 카드 bbox 기준 상하/좌우 여백을 계산한다.
- 외곽 테두리 안의 전체 component bbox도 별도로 계산해 그림 전체가 한쪽으로 치우치지 않게 한다.

## 결과

루트 README의 세 다이어그램을 다시 배치했다.

- `root-readme-overview-01`
- `root-readme-module-chart-01`
- `bluetape4k-text-architecture-01`

기존 `docs/assets/readme-*` 산출물은 제거하고, SVG/PNG를 모두 `docs/images/readme-diagrams`로 통일했다.

## 검증

- 세 root SVG 파일 전체에 대한 `xmllint --noout`
- 세 root SVG 파일 전체에 대한 CairoSVG 렌더링
- Line audit: diagonal segments `0`
- Marker audit: arrowhead color mismatches `0`
- Bbox audit: layer/card margin과 outer component margin
- Rendered PNG contact sheet visual inspection
- `git diff --check`

## 향후 지침

다이어그램 검증을 "기계 검사 후 육안 확인"으로 끝내지 말고, 같은 이미지에서 세 가지 bbox를 모두 확인한다.

1. card 내부 text bbox
2. layer 내부 card bbox
3. outer frame 내부 component bbox

하나만 맞으면 된다고 판단하면 다음 수정에서 다른 균형이 다시 깨진다.
