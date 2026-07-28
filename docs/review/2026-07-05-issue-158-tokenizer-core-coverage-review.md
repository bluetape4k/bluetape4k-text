# Issue #158 tokenizer-core coverage 검토

## 범위

- 저장소: bluetape4k-text
- 이슈: #158 `test(tokenizer-core): raise coverage above repo module average`
- 모듈: `:tokenizer-core`
- 목표: 저장소 module average 81.64% instruction coverage 초과.

## 검토한 변경

- Tokenizer exception hierarchy에 대한 constructor coverage를 추가했다.
- Unmodifiable view, companion copy, empty singleton behavior, entry iteration, entry-set mutation guard, key-set mutation guard, string rendering에 대한 `CharArrayMap` test를 추가했다.
- Companion copy, unmodifiable view, string rendering에 대한 `CharArraySet` test를 추가했다.
- `CharacterBuffer` wrapping, trailing high-surrogate carry-over, invalid fill size, negative conversion length에 대한 `CharacterUtils` test를 추가했다.

## Coverage 증거

- `tokenizer-core/build/reports/kover/report.xml` 기준선: 1927/2698 instructions = 71.42%.
- Test 추가 후: 2315/2698 instructions = 85.80%.
- 결과: PASS, module이 81.64% repo module average를 +4.16 percentage point 초과한다.

## 검증

- PASS: `./gradlew :tokenizer-core:test :tokenizer-core:koverXmlReport --no-daemon --no-configuration-cache`
- PASS: `./gradlew :tokenizer-core:check :tokenizer-core:koverXmlReport --no-daemon --no-configuration-cache`

## 검토 판정

- P0/P1 finding: 없음.
- Public API 변경: 없음.
- Production behavior 변경: 없음.
- Testcontainers/concurrency helper: 해당 없음. 이 모듈은 순수 JVM utility/model test를 사용한다.
