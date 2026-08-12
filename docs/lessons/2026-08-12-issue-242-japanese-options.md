# 일본어 BlockwordOptions locale/severity 계약

## 배경

Issue #242에서 공용 `BlockwordOptions`의 `locale`과 `severity`가 일본어
마스킹 경로에서 무시되던 결함을 수정했다. 일본어 processor는 이전에
`mask`만 읽고 flat `Set<String>` 사전 전체를 항상 검사했다.

## 근본 원인

일본어 사전 snapshot이 severity tier 정보를 보존하지 않았고,
`JapaneseBlockwordProcessor.maskBlockwords`가 locale 검증이나 severity 조회를
수행하지 않았다. 따라서 한국어와 달리 잘못된 locale이 조용히 허용되고
threshold를 지정해도 같은 결과가 반환됐다.

## 결정

- 기존 `blocks.txt` 전체 목록은 유지하고 `blocks_severity.tsv`에 exact tier
  override를 기록한다. provider는 이를 `LOW=전체`, `MIDDLE=middle/high`,
  `HIGH=high` cumulative view로 정규화한다.
- 일본어 masking은 `Locale.JAPANESE`만 허용하고, 다른 locale은
  `InvalidTokenizeRequestException`으로 거부한다.
- tier를 생략한 runtime 추가는 기존 호출 호환성을 위해 LOW에 기록한다.
  필요하면 facade/provider의 severity overload로 exact tier를 지정한다.
- 기존 flat `currentBlockwordSnapshot()`, `blockWordDictionary`, 그리고
  `readWords("block/blocks.txt")`의 파일 형식은 유지하고,
  severity-aware 경로는 별도 snapshot API로 제공해 기존 호출자의 타입 계약을
  깨지 않는다.

## 검증

- locale 거부 회귀 테스트가 수정 전 `Expected InvalidTokenizeRequestException but no exception was thrown`으로 RED였다.
- severity threshold 회귀 테스트가 수정 전 severity별 결과 불일치로 RED였다.
- 수정 후 `:tokenizer-japanese:test` 93건 PASS.
- `git diff --check` 및 module detekt/compile 검증은 최종 단계에서 재실행한다.

## 향후 지침

새 일본어 금칙어를 추가할 때는 resource의 exact tier와 cumulative threshold
계약을 함께 검토한다. `BlockwordOptions`를 소비하는 새 facade는 locale을
검증하고, flat dictionary를 직접 조회해 severity를 우회하지 않는다.
