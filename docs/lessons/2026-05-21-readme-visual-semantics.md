# README 시각 의미 구조

## 배경

Root README visual overview는 있었지만 README module table은 BOM project를 빠뜨렸다.
또한 visual grouping order 때문에 core tokenizer model layer가 보조 module보다 덜
두드러졌다.

## 결정

첫 README visual은 영문 전용 overview로 유지한다. Module orientation 순서는 BOM,
core model, language detection, search, Japanese tokenizer, Korean tokenizer로 둔다.
Public dependency coordinate는 `projectGroup=io.github.bluetape4k.text`와 현재 Gradle
artifact name을 기준으로 정규화한다.

## 결과

Root README module table을 업데이트하고, root overview와 module chart PNG를 다시 생성했다.
Localized diagram alt text를 고쳤고, README 파일에서 오래된 `io.bluetape4k:*`
dependency example을 제거했다.

## 검증

- SVG source에서 업데이트된 PNG asset을 `rsvg-convert`로 다시 생성했다.
- 업데이트된 root SVG asset은 `xmllint --noout`를 통과했다.
- `./gradlew -q projects`로 현재 project를 확인했다: `:bluetape4k-text-bom`,
  `:tokenizer-core`, `:lingua`, `:text-search`, `:tokenizer-japanese`, `:tokenizer-korean`.
- Visual inspection으로 가운데 정렬된 label과 읽을 수 있는 layout을 확인했다.

## 향후 지침

Text README를 업데이트할 때 dependency coordinate의 source of truth는
`settings.gradle.kts`와 `gradle.properties`다. Module-level example에 남아 있는 오래된
branded module name을 기준으로 삼지 않는다.
