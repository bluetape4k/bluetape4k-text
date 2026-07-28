# README Hero와 아키텍처 갱신

## 배경

Text 저장소에는 아키텍처 문서가 있었지만 시각적 진입점, 명시적인 목적/기능 설명,
최신 WIP snapshot이 부족했다.

## 결정

생성된 text-processing workbench 이미지를 `docs/assets/text-workbench.png`에 저장하고,
README 언어 전환 위치를 정규화하며, 이 snapshot 시점에 할당된 open issue가 없음을
보여주는 `WIP.md`를 만든다.

## 결과

두 README locale은 이제 모듈 표보다 먼저 tokenizer, language detection, dictionary,
Aho-Corasick 범위를 소개한다.

## 검증

- 생성된 asset이 `docs/assets` 아래 PNG로 존재함을 확인했다.
- 두 README locale이 공유 image path를 참조함을 확인했다.

## 향후 지침

WIP snapshot은 issue 중심으로 유지한다. 할당된 issue가 없으면 backlog를 꾸며내지
말고 그 사실을 명시적으로 기록한다.
