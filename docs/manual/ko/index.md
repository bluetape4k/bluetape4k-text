# bluetape4k-text 0.2 매뉴얼

`bluetape4k-text`는 한국어·일본어 토큰화, 언어 감지, 사전 기반 필터링, 다중 패턴 검색을 제공하는 Kotlin/JVM 라이브러리다. 이 매뉴얼은 안정 배포본 `0.3.0`을 기준으로 하며, 저장소 디렉터리보다 사용자가 해결하려는 문제에서 출발한다.

![저장소 학습 지도](../assets/overview/repository-learning-map.png)

## 핵심 기능

| 필요한 일 | 먼저 읽을 문서 | 실행하며 배울 예제 |
|---|---|---|
| 한국어를 정규화·토큰화하거나 어간·구문을 추출한다 | [한국어 토크나이저](modules/tokenizer-korean.md) | [토크나이저 안전 예제](examples/tokenizer-safety-examples.md) |
| 일본어를 토큰화하고 품사를 살펴본다 | [일본어 토크나이저](modules/tokenizer-japanese.md) | [토크나이저 안전 예제](examples/tokenizer-safety-examples.md) |
| 한 문장에 포함된 언어를 하나 이상 찾는다 | [Lingua](modules/lingua.md) | [Lingua 예제](examples/lingua-examples.md) |
| 도메인 사전으로 불용어를 걸러 낸다 | [사전과 불용어](guides/dictionaries-and-blockwords.md) | [토크나이저 안전 예제](examples/tokenizer-safety-examples.md) |
| 여러 키워드를 한 번에 찾는다 | [텍스트 검색](modules/text-search.md) | [텍스트 검색 예제](examples/text-search-examples.md) |
| 자체 토크나이저나 요청 경계를 만든다 | [Tokenizer Core](modules/tokenizer-core.md) | [입력 안전성](guides/input-safety.md) |
| Text 모듈의 버전을 한꺼번에 맞춘다 | [Text BOM](modules/bluetape4k-text-bom.md) | [시작하기](getting-started.md) |

어떤 기능을 골라야 할지 애매하다면 [기능 선택 가이드](guides/capability-selection.md)를 먼저 읽자. 언어 감지, 토큰화, 사전 필터링, 정확한 문자열 검색을 서로 다른 기능으로 비교하고 필요한 경우 조합하는 방법을 설명한다.

## Build

[시작하기](getting-started.md)는 BOM 적용부터 첫 한국어 토큰화 결과까지 가장 짧은 경로를 제공한다. 일반 사용자는 `bluetape4k-dependencies` 버전만 선택하면 되고, Text 전용 BOM은 전체 생태계 버전을 별도로 관리할 때만 사용하면 된다.

저장소는 BOM 하나와 런타임 라이브러리 다섯 개를 배포한다. BOM에는 실행 코드가 없으므로 사용할 모듈을 의존성에 따로 추가해야 한다. 규모가 큰 서비스를 설계한다면 [저장소 지도](architecture/repository-map.md)에서 모듈과 예제의 경계를 먼저 확인하자.

## Learn

[학습 경로](guides/learning-path.md)는 링크만 나열하지 않는다. 각 단계에서 무엇을 배우고, 어떤 예제를 실행하며, 어떤 결과를 확인한 뒤 다음 단계로 넘어갈지 알려준다.

1. 프로세서 하나를 빌드하고 호출한다.
2. Unicode 문자 범위와 통계 기반 언어 감지 중 알맞은 방식을 고른다.
3. 한국어·일본어 프로세서의 역할과 차이를 이해한다.
4. 불변 Aho-Corasick automaton을 구성한다.
5. 요청 제한과 메모리, 실패 처리 방식을 운영 환경에 맞춘다.

모듈 문서에는 실행 가능한 최소 코드, 결과 해석, 선택 기준, 제약 사항, 안정 배포본 소스 링크가 들어 있다. 세 예제 문서도 디렉터리 링크에 그치지 않고 코드의 의미와 수정해 볼 지점을 자세히 설명한다.

## Operate

언어 모델과 토크나이저 사전, 검색 automaton은 초기화 비용과 메모리 사용 방식이 서로 다르다. HTTP 요청을 받기 전에 [런타임 경계](architecture/runtime-boundaries.md), [시작과 메모리](operations/startup-and-memory.md), [실패 계약](operations/failure-contracts.md)을 읽어 두자.

[품질 게이트](quality/quality-gates.md)는 저장소가 실제로 검증한 내용과 아직 주장하지 않는 내용을 구분한다. [Aho-Corasick 벤치마크](quality/aho-corasick-benchmarks.md)는 측정 환경과 해석 기준을 함께 기록해 로컬 수치를 운영 성능 순위로 오해하지 않도록 돕는다.

## 버전과 소스

이 매뉴얼은 `0.2` 마이너 계열을 다루며 안정 배포본 `0.3.0`, 커밋 `aead213d2d25307d7d3684226943a5f95c7411f2`에 고정되어 있다. 소스 링크도 같은 배포본을 가리키므로 이후 개발 내용이 현재 설명을 바꾸지 않는다.

- [배포본 README](https://github.com/bluetape4k/bluetape4k-text/blob/0.3.0/README.md)
- [배포본 프로젝트 설정](https://github.com/bluetape4k/bluetape4k-text/blob/0.3.0/settings.gradle.kts)
