# 처리 모델

언어 감지, 토큰화, 사전 필터링, 패턴 검색은 서로 다른 문제를 푼다. 요청에 필요한 단계만 골라 조합하자.

![텍스트 처리 흐름](../../assets/architecture/text-processing-pipeline.png)

## 1단계: 입력을 받고 제한하기

언어 모델이나 사전을 사용하기 전에 텍스트를 검증한다. `tokenizeRequestOf`와 `blockwordRequestOf`는 빈 입력을 거부하고 각 요청의 최대 길이를 검사한다. HTTP adapter는 이를 `400`과 `413`으로 변환하되 제출된 내용을 응답에 포함하지 않는다.

신뢰할 수 없는 외부 요청이라면 이 단계가 반드시 필요하다. 다만 요청이 유효하다는 사실은 언어를 정확히 알거나 형태소 분석이 성공한다는 뜻이 아니다.

## 2단계: 라우팅에 필요한 문자 범위나 언어 찾기

`UnicodeDetector`는 지원하는 문자 범위가 포함됐는지, 어떤 문자가 해당하는지 결정적으로 알려준다. Lingua `LanguageDetector`는 텍스트에서 언어를 추정하고 `detectAllLanguagesOf`로 혼합 입력을 살펴본다.

간단한 문자 범위 분기에는 Unicode 필터를 사용하고, 라틴 문자를 공유하는 언어나 애매한 텍스트까지 구분해야 한다면 통계 기반 감지기를 사용한다. 두 방식을 함께 써서 문자 범위로 빠른 경로를 고르고 나머지만 Lingua로 처리할 수도 있다.

## 3단계: 언어별 프로세서 호출하기

`KoreanProcessor`와 `JapaneseProcessor`는 내부 토크나이저를 한곳에서 사용할 수 있게 만든 facade다. 한국어 모듈은 정규화, 어간·구문 추출, 문장 분리, detokenization도 제공한다. 일본어 모듈은 Kuromoji 토큰과 품사 필터를 제공한다.

품사, 위치, 어간을 뒤에서 사용한다면 토큰을 너무 일찍 문자열로 줄이지 말자. 문자열 변환은 화면 표시나 단순 결과가 필요한 마지막 경계에서 수행한다.

## 4단계: 사전 정책 적용하기

두 프로세서 모두 금칙어 기능을 제공하지만 어떤 단어를 어떤 등급으로 관리할지는 애플리케이션 정책이다. 런타임 추가 항목은 같은 프로세스의 이후 요청에 영향을 준다. 업데이트의 인증, 검증, 배포, 재시작 후 복구 방식을 정해야 한다.

`tokenizer-core`의 `DictionaryProvider`는 여러 리소스 사전을 비동기로 합쳐 읽는다. 이는 초기화 작업이며 요청마다 반복할 작업이 아니다.

## 5단계: 정확한 패턴 검색하기

`AhoCorasickAutomaton<V>`는 키워드에 애플리케이션 값을 연결한다. 한 번 빌드한 뒤 `parseText`, `firstMatch`, `tokenize`, `replaceAll`, `matchesAsFlow`를 사용한다. `build()` 이후에는 불변이므로 여러 요청에서 공유할 수 있다.

Aho-Corasick 검색 전에 반드시 형태소 분석을 할 필요는 없다. 분석한 토큰 형태에 따라 검색어가 달라질 때만 두 기능을 조합하자. 그렇지 않으면 원문 검색이 불필요한 비용을 줄이고 원래 위치를 보존한다.

## 결과와 실패의 책임

- 요청 검증은 잘못된 입력과 길이 초과를 맡는다.
- 언어 감지는 가능성과 모호함을 다루며 진실을 보장하지 않는다.
- 토크나이저는 분석 결과와 프로세서 오류를 맡는다.
- 사전은 정책 일치와 마스킹을 맡는다.
- 검색기는 정확한 일치 위치와 연결된 값을 반환한다.

이 경계를 분명히 하면 테스트와 오류 변환이 단순해진다. 다음으로 [런타임 경계](runtime-boundaries.md), [테스트](../guides/testing.md), [실패 계약](../operations/failure-contracts.md)을 읽어 보자.

## 소스 근거

- [토크나이저 요청 모델](https://github.com/bluetape4k/bluetape4k-text/tree/0.3.0/tokenizer-core/src/main/kotlin/io/bluetape4k/tokenizer/model)
- [Lingua 소스](https://github.com/bluetape4k/bluetape4k-text/tree/0.3.0/lingua/src/main/kotlin/io/bluetape4k/lingua)
- [Aho-Corasick 구현](https://github.com/bluetape4k/bluetape4k-text/blob/0.3.0/text-search/src/main/kotlin/io/bluetape4k/text/search/AhoCorasickAutomaton.kt)
