# 저장소 지도

저장소는 재사용할 런타임 라이브러리와 실행 예제를 분리한다. 위쪽 여섯 개 모듈만 배포하며, 세 예제는 CI smoke test이자 학습 자료로 사용한다.

![기능과 모듈 지도](../../assets/architecture/capability-map.png)

## 배포 모듈

| 모듈 | 제공하는 기능 | 다른 Text 모듈 의존성 |
|---|---|---|
| `bluetape4k-text-bom` | Text 모듈 버전 정렬 | 런타임 모듈 다섯 개의 버전을 제약한다 |
| `tokenizer-core` | 요청·응답 모델, 옵션, 심각도, 사전, 문자 컬렉션 | 없음 |
| `tokenizer-korean` | 한국어 정규화, 토큰화, 어간·구문 추출, 문장 분리, 금칙어 | `tokenizer-core` |
| `tokenizer-japanese` | Kuromoji IPAdic 토큰화, 품사 필터, 금칙어 | `tokenizer-core` |
| `lingua` | Lingua 감지기, 혼합 언어 확장, Unicode 문자 범위 필터 | 필수 Text 런타임 의존성 없음 |
| `text-search` | 불변 Aho-Corasick automaton, DSL, 치환, Flow 검색 | 필수 Text 런타임 의존성 없음 |

BOM에는 실행 코드가 없다. `tokenizer-core`는 프로세서와 안전한 요청 경계의 기반이며, 일반 애플리케이션은 core만 직접 쓰기보다 한국어 또는 일본어 프로세서를 선택하는 경우가 많다.

## 실행 예제

| Gradle 프로젝트 | 확인할 수 있는 내용 |
|---|---|
| `:examples:text-search-examples` | builder와 DSL 결과, 문자열 치환, 첫 Flow 경고 |
| `:examples:lingua-examples` | 감지기 재사용, 언어 집합 제한, 저정확도 모드 |
| `:examples:tokenizer-safety-examples` | 빈 입력·초과 입력·프로세서 실패를 400/413/500으로 안전하게 변환 |

```bash
./gradlew :examples:text-search-examples:run \
  :examples:lingua-examples:run \
  :examples:tokenizer-safety-examples:run
```

예제 모듈은 Maven Central에 배포하지 않는다. 예제가 보여 주는 경계와 조합 방식을 애플리케이션 코드에 적용하고, 예제 자체를 의존성으로 추가하지 않는다.

## 자주 쓰는 조합

### 언어에 따라 분기하기

`lingua`로 후보 언어를 찾은 다음 지원하는 텍스트를 한국어 또는 일본어 프로세서에 보낸다. 언어를 알 수 없거나 결과가 애매한 경우를 위한 분기도 반드시 둔다. [혼합 언어 처리](../guides/mixed-language-processing.md)는 감지 결과를 절대적인 사실이 아니라 라우팅 근거로 사용하는 방법을 설명한다.

### 토큰화하고 정책 사전 적용하기

형태소 분석과 내장 금칙어 기능은 언어별 프로세서가 맡는다. 외부 요청 경계에서는 `tokenizer-core` 모델을 사용한다. [사전과 금칙어](../guides/dictionaries-and-blockwords.md)에서 런타임 업데이트와 소유권을 다룬다.

### 형태소 분석 없이 정확한 문자열 찾기

이미 검색할 패턴을 알고 있고 한 번에 여러 개를 찾아야 한다면 `text-search`를 사용한다. 원문을 직접 검색하므로 토크나이저와 독립적으로 동작한다. 입력 계약에 필요한 정규화와 단어 경계 옵션만 명시적으로 켠다.

## 소스 근거

- [배포본 프로젝트 등록](https://github.com/bluetape4k/bluetape4k-text/blob/0.3.0/settings.gradle.kts)
- [배포본 모듈 소개](https://github.com/bluetape4k/bluetape4k-text/blob/0.3.0/README.md)
- [예제 소개](https://github.com/bluetape4k/bluetape4k-text/blob/0.3.0/examples/README.md)

<!-- release-readme-diagrams:start -->
## 배포본 다이어그램 {#release-diagrams}

아래 그림은 `0.3.0` 배포본의 README 자산을 해당 배포 커밋에서 직접 불러옵니다. 이후 SNAPSHOT이 아니라 이 매뉴얼 버전의 구조와 실행 흐름을 보여 줍니다. 미리보기를 누르면 같은 배포 커밋의 SVG 원본이 열립니다.

### Bluetape4k Text 아키텍처

[![Bluetape4k Text 아키텍처](https://raw.githubusercontent.com/bluetape4k/bluetape4k-text/aead213d2d25307d7d3684226943a5f95c7411f2/docs/images/readme-diagrams/bluetape4k-text-architecture-01.png)](https://github.com/bluetape4k/bluetape4k-text/blob/aead213d2d25307d7d3684226943a5f95c7411f2/docs/images/readme-diagrams/bluetape4k-text-architecture-01.svg)

_배포본 README: [`README.ko.md`](https://github.com/bluetape4k/bluetape4k-text/blob/aead213d2d25307d7d3684226943a5f95c7411f2/README.ko.md)_

### Bluetape4k Text 모듈 구성도

[![Bluetape4k Text 모듈 구성도](https://raw.githubusercontent.com/bluetape4k/bluetape4k-text/aead213d2d25307d7d3684226943a5f95c7411f2/docs/images/readme-diagrams/root-readme-module-chart-01.png)](https://github.com/bluetape4k/bluetape4k-text/blob/aead213d2d25307d7d3684226943a5f95c7411f2/docs/images/readme-diagrams/root-readme-module-chart-01.svg)

_배포본 README: [`README.ko.md`](https://github.com/bluetape4k/bluetape4k-text/blob/aead213d2d25307d7d3684226943a5f95c7411f2/README.ko.md)_

### Bluetape4k Text 개요

[![Bluetape4k Text 개요](https://raw.githubusercontent.com/bluetape4k/bluetape4k-text/aead213d2d25307d7d3684226943a5f95c7411f2/docs/images/readme-diagrams/root-readme-overview-01.png)](https://github.com/bluetape4k/bluetape4k-text/blob/aead213d2d25307d7d3684226943a5f95c7411f2/docs/images/readme-diagrams/root-readme-overview-01.svg)

_배포본 README: [`README.ko.md`](https://github.com/bluetape4k/bluetape4k-text/blob/aead213d2d25307d7d3684226943a5f95c7411f2/README.ko.md)_

<!-- release-readme-diagrams:end -->
