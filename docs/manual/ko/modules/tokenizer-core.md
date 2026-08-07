# 토크나이저 핵심 라이브러리

`tokenizer-core`는 공통 요청·응답 모델, 정책 옵션, 사전 loader, 문자 컬렉션, 토크나이저 예외를 제공한다. 외부 요청 경계나 자체 프로세서를 만들 때 직접 사용하고, 한국어·일본어 프로세서만 호출한다면 보통 전이 의존성으로 받는다.

## 제공하는 기능

- `TokenizeRequest`, `TokenizeResponse`, `BlockwordRequest`, `BlockwordResponse`
- locale·마스킹 옵션과 `Severity.LOW`, `MIDDLE`, `HIGH`
- 0.3.0에서 모두 `100_000`인 `MAX_TOKENIZE_TEXT_LENGTH`, `MAX_BLOCKWORD_TEXT_LENGTH`
- 일반 텍스트·gzip classpath 사전을 읽는 `DictionaryProvider`
- 문자 시퀀스를 반복 조회하기 위한 `CharArraySet`, `CharArrayMap`
- `TokenizerException`, `InvalidTokenizeRequestException`

## 의존성 추가하기

```kotlin
dependencies {
    implementation("io.github.bluetape4k.text:tokenizer-core:0.3.0")
}
```

BOM이 버전을 관리한다면 좌표에서 버전을 뺀다.

## 가장 작은 예제

```kotlin
import io.bluetape4k.tokenizer.model.TokenizeOptions
import io.bluetape4k.tokenizer.model.tokenizeRequestOf
import java.util.Locale

val request = tokenizeRequestOf(
    text = "코틀린 코루틴",
    options = TokenizeOptions(locale = Locale.KOREAN),
)

println(request.text)
println(request.options.locale)
```

factory는 프로세서가 호출되기 전에 텍스트를 검사하고 공통 메시지 계약에 필요한 메타데이터를 만든다.

## 사전 로딩

```kotlin
import io.bluetape4k.tokenizer.utils.DictionaryProvider

val words = DictionaryProvider.readWords("dict/base.txt", "dict/custom.txt")
println("blocked" in words)
```

`readWords`는 여러 리소스를 coroutine 기반 비동기 경로로 읽어 `CharArraySet`으로 반환한다. 초기화 과정에서 한 번 호출하고 결과를 재사용하자. `readWordsAsSequence`는 지연 line reader이며, `readWordFreqs`는 탭으로 구분한 단어·빈도 데이터를 읽는다.

## 선택 기준

HTTP나 메시지 요청 모델, 공통 사전 도구, 자체 프로세서가 필요하다면 core를 직접 사용한다. 실제 형태소 분석이 목적이라면 [한국어](tokenizer-korean.md) 또는 [일본어](tokenizer-japanese.md) 모듈을 선택한다.

## 제약과 실패

빈 문자열과 `100_000`자를 넘는 텍스트는 처리 전에 거부된다. 오류 응답과 로그에 제출 원문을 포함하지 않는다. 사전 리소스를 읽지 못한 오류는 초기화 실패로 다루고 요청마다 반복해서 읽지 않는다.

## 다음 학습 경로

- [입력 안전성](../guides/input-safety.md)
- [사전과 금칙어](../guides/dictionaries-and-blockwords.md)
- [토크나이저 안전 예제](../examples/tokenizer-safety-examples.md)

## 소스 근거

- [TokenizeRequest](https://github.com/bluetape4k/bluetape4k-text/blob/0.3.0/tokenizer-core/src/main/kotlin/io/bluetape4k/tokenizer/model/TokenizeRequest.kt)
- [BlockwordRequest](https://github.com/bluetape4k/bluetape4k-text/blob/0.3.0/tokenizer-core/src/main/kotlin/io/bluetape4k/tokenizer/model/BlockwordRequest.kt)
- [DictionaryProvider](https://github.com/bluetape4k/bluetape4k-text/blob/0.3.0/tokenizer-core/src/main/kotlin/io/bluetape4k/tokenizer/utils/DictionaryProvider.kt)

<!-- release-readme-diagrams:start -->
## 배포본 다이어그램 {#release-diagrams}

아래 그림은 `0.3.0` 배포본의 README 자산을 해당 배포 커밋에서 직접 불러옵니다. 이후 SNAPSHOT이 아니라 이 매뉴얼 버전의 구조와 실행 흐름을 보여 줍니다. 미리보기를 누르면 같은 배포 커밋의 SVG 원본이 열립니다.

### tokenizer core 클래스 구조도

[![tokenizer core 클래스 구조도](https://raw.githubusercontent.com/bluetape4k/bluetape4k-text/aead213d2d25307d7d3684226943a5f95c7411f2/docs/images/readme-diagrams/tokenizer-core-class-01.png)](https://github.com/bluetape4k/bluetape4k-text/blob/aead213d2d25307d7d3684226943a5f95c7411f2/docs/images/readme-diagrams/tokenizer-core-class-01.svg)

_배포본 README: [`tokenizer-core/README.ko.md`](https://github.com/bluetape4k/bluetape4k-text/blob/aead213d2d25307d7d3684226943a5f95c7411f2/tokenizer-core/README.ko.md)_

<!-- release-readme-diagrams:end -->
