# 기능 선택

애플리케이션이 받아야 할 결과부터 정하자. 언어 감지, 토큰화, 사전 정책, 정확한 문자열 검색은 서로 보완하지만 각각 독립된 기능이다.

## 선택표

| 요구 사항 | 주 모듈 | 대표 API | 실행 근거 |
|---|---|---|---|
| Text 모듈 버전을 맞춘다 | `bluetape4k-text-bom` | Gradle `platform(...)` | [시작하기](../getting-started.md) |
| 토크나이저 요청 경계를 검증한다 | `tokenizer-core` | `tokenizeRequestOf`, `blockwordRequestOf` | [안전 예제](../examples/tokenizer-safety-examples.md) |
| 한국어를 정규화하거나 형태소 분석한다 | `tokenizer-korean` | `KoreanProcessor` | [한국어 모듈](../modules/tokenizer-korean.md) |
| 일본어를 형태소 분석한다 | `tokenizer-japanese` | `JapaneseProcessor` | [일본어 모듈](../modules/tokenizer-japanese.md) |
| 언어를 추정하거나 문자 범위로 거른다 | `lingua` | `LanguageDetector`, `UnicodeDetector` | [Lingua 예제](../examples/lingua-examples.md) |
| 여러 정확한 패턴을 찾거나 바꾼다 | `text-search` | `AhoCorasickAutomaton` | [검색 예제](../examples/text-search-examples.md) |

## 언어 감지와 토큰화

언어 감지는 “어떤 언어가 들어 있을 가능성이 높은가?”를 답한다. 토큰화는 “이 언어의 규칙으로 어떻게 나누고 분류하는가?”를 답한다. 감지기는 형태소를 만들지 않고, 토크나이저는 임의의 입력 언어를 증명하지 않는다.

endpoint가 한국어 전용이라면 경계를 검증하고 한국어 프로세서를 바로 호출한다. 입력 언어를 모를 때만 감지를 추가한다.

## 토큰화와 정확한 검색

품사, 어간, 언어별 정규화가 필요하다면 토크나이저를 사용한다. 이미 알고 있는 문자열 패턴을 여러 개 한 번에 찾아야 한다면 Aho-Corasick을 사용한다. 원문을 직접 검색하면 형태소 초기화 없이 원래 위치를 보존할 수 있다.

moderation 서비스가 두 기능을 함께 사용할 수도 있다. 형태소 기반 정책 결과와 정확한 패턴 결과를 분리해 어느 규칙이 일치했는지 테스트에서 알 수 있게 한다.

## Unicode 필터와 Lingua

“한글이 포함됐는가?”처럼 문자 범위를 묻는다면 `UnicodeDetector`를 사용한다. Latin 문자를 공유하는 자연어를 구분하려면 Lingua가 알맞다. 혼합 입력에서는 문자 범위로 빠른 1차 분기를 하고 나머지만 통계 감지기로 보낼 수 있다.

## Core를 직접 추가할 때

외부 API에서 core 모델이나 사전 도구를 직접 노출한다면 `tokenizer-core`를 명시한다. 한국어·일본어 프로세서 내부에서만 사용한다면 전이 의존성으로 두어도 된다.

## 선택 체크리스트

1. 필요한 결과가 언어 집합, 분석 token, 정책 일치, 정확한 위치 중 무엇인지 적는다.
2. endpoint 계약에 입력 언어가 이미 포함됐는지 확인한다.
3. 초기화 상태를 요청 사이에서 공유할 수 있는지 결정한다.
4. 지원 언어와 사전 범위를 최소화한다.
5. 다른 모듈을 조합하기 전에 경계·모호함·실패 테스트를 만든다.

[처리 모델](../architecture/processing-model.md)에서 조합 방식을, [런타임 경계](../architecture/runtime-boundaries.md)에서 수명 선택을 확인할 수 있다.
