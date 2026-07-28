# Issue #157 lingua coverage 검토

## 범위

- 저장소: bluetape4k-text
- 이슈: #157 `test(lingua): raise coverage above repo module average`
- 모듈: `:lingua`
- 목표: 저장소 module average 81.64% instruction coverage 초과.

## 검토한 변경

- `Char.isAscii`, `isLatin`, `isArabic`, `isThai`, `isKorean`, `isJapanese`, `isChinese`에 대한 직접 Unicode block coverage test를 추가했다.
- 지원 locale, `filterChar`, `filterString` locale filtering에 대한 `UnicodeDetector` test를 추가했다.
- All-spoken detector 생성, low-accuracy overload, Latin phrase candidate correction, single-letter Latin-token filtering, 혼합 text의 non-preferred long Latin-token filtering을 위한 `LanguageDetector` builder/extension test를 추가했다.

## Coverage 증거

- `lingua/build/reports/kover/report.xml` 기준선: 816/1120 instructions = 72.86%.
- Test 추가 후: 1005/1120 instructions = 89.73%.
- 결과: PASS, module이 81.64% repo module average를 +8.09 percentage point 초과한다.

## 검증

- PASS: `./gradlew :lingua:test :lingua:koverXmlReport --no-daemon --no-configuration-cache`
- PASS: `./gradlew :lingua:check :lingua:koverXmlReport --no-daemon --no-configuration-cache`

## 검토 판정

- P0/P1 finding: 없음.
- Public API 변경: 없음.
- Production behavior 변경: 없음.
- Testcontainers/concurrency helper: 해당 없음. 이 모듈은 순수 JVM language-detection/unit test를 사용한다.
