# 테스트

텍스트 처리 테스트는 사용자에게 보이는 계약을 고정하되 내부 형태소 선택 전체를 얼려서는 안 된다. 요청 경계, 라우팅, 토큰, 정책, 검색, 성능 검증을 나눠 작성한다.

## 요청 경계 테스트

토큰화와 금칙어 요청에서 다음을 확인한다.

- 빈 문자열과 공백 문자열
- 정확히 `100_000`자
- `100_001`자
- 제출 원문의 고유 sentinel이 오류 메시지에 없는지

adapter의 상태 변환과 모델 예외를 따로 테스트하면 transport 정책과 라이브러리 방어 중 어느 쪽이 실패했는지 알 수 있다.

## 한국어·일본어 fixture

대표 혼합 텍스트의 안정적인 surface token을 검증한다. 제품이 품사나 어간에 의존할 때만 해당 assertion을 추가한다. 배포 품질 게이트도 사용자에게 보이는 결과를 중심으로 하며 내부 형태소 선택 전부에 묶이지 않는다.

한국어는 정규화, 런타임 명사, 어간, 문장 분리, 금칙어 등급을 포함한다. 일본어는 명사·동사 필터, 복합 금칙어, 런타임 사전 변경, 마스킹을 포함한다.

## 언어 감지 테스트

제품이 지원하는 언어 집합으로 감지기를 한 번 만들고 fixture에서 재사용한다. 언어별 명확한 문장, 한국어·일본어·Latin 혼합 문장, 짧고 모호한 Latin 입력, 빈 입력, 빈 집합·복수 언어의 라우팅 정책을 검증한다.

감지 집합과 애플리케이션 route를 따로 assertion한다. Set 출력 순서는 계약이 아니다.

## Aho-Corasick 테스트

```kotlin
val automaton = ahoCorasick<String> {
    allowOverlaps = false
    keyword("he", "HE")
    keyword("hers", "HERS")
}

val values = automaton.parseText("hers").map { it.value }
```

겹치는 키워드, no-match, 대소문자 무시, 각 단어 경계, NFC/NFKC 위치 mapping, 치환, `take(1)` Flow 수집을 다룬다. 배포된 snapshot은 변경하지 말고 새 automaton을 빌드해 교체하는 방식으로 테스트한다.

## 성능 근거

기능 테스트는 정확성을 답하고 JMH는 기록된 환경에서 상대 throughput을 답한다. 로컬 `ops/s` 하나를 단위 테스트의 절대 임계값으로 만들지 않는다. 명령, JDK, fixture, warmup, 측정 mode, 지표 방향이 같을 때만 비교한다.

[품질 게이트](../quality/quality-gates.md)에서 배포 증거 범위를, [Aho-Corasick 벤치마크](../quality/aho-corasick-benchmarks.md)에서 검색 성능 사례를 확인할 수 있다.

## 소스 근거

- [한국어 프로세서 테스트](https://github.com/bluetape4k/bluetape4k-text/blob/0.3.0/tokenizer-korean/src/test/kotlin/io/bluetape4k/tokenizer/korean/KoreanProcessorTest.kt)
- [일본어 프로세서 테스트](https://github.com/bluetape4k/bluetape4k-text/blob/0.3.0/tokenizer-japanese/src/test/kotlin/io/bluetape4k/tokenizer/japanese/JapaneseProcessorTest.kt)
- [안전 예제 테스트](https://github.com/bluetape4k/bluetape4k-text/blob/0.3.0/examples/tokenizer-safety-examples/src/test/kotlin/io/bluetape4k/text/examples/tokenizer/TokenizerSafetyExamplesTest.kt)
