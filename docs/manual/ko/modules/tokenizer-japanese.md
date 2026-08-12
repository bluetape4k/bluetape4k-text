# 일본어 토크나이저 라이브러리

`tokenizer-japanese`는 Kuromoji IPAdic을 `JapaneseProcessor`로 감싼다. 형태소 분석, 품사 판정과 필터, 복합어를 고려한 금칙어 탐지·마스킹, 런타임 사전 관리를 제공한다.

## 제공하는 기능

- `TokenBase`를 반환하는 Kuromoji IPAdic 토큰화
- `filterNoun`과 predicate 기반 `filter`
- `isNoun`, `isVerb`, `isNounOrVerb`, `isAdjective`, `isJosa`, `isPunctuation`
- 명사·동사 중심 금칙어 탐지와 인접 복합어 검사
- 메모리 금칙어 사전의 추가·삭제·전체 삭제

## 의존성 추가하기

```kotlin
dependencies {
    implementation("io.github.bluetape4k.text:tokenizer-japanese:0.3.0")
}
```

## 가장 작은 예제

```kotlin
import io.bluetape4k.tokenizer.japanese.JapaneseProcessor

val tokens = JapaneseProcessor.tokenize("お寿司が食べたい。")
println(tokens.map { it.surface })
// [お, 寿司, が, 食べ, たい, 。]
```

토큰에는 사전과 품사 정보가 남아 있다. 표시 문자열만 필요할 때 `surface`를 꺼낸다.

## 품사로 걸러내기

```kotlin
import io.bluetape4k.tokenizer.japanese.tokenizer.isVerb

val analyzed = JapaneseProcessor.tokenize("私は、日本語の勉強をしています。")
val nouns = JapaneseProcessor.filterNoun(analyzed).map { it.surface }
val verbs = JapaneseProcessor.filter(analyzed) { it.isVerb() }.map { it.surface }

println(nouns) // [私, 日本語, 勉強]
println(verbs) // [し]
```

정책이 문법 범주를 기준으로 한다면 원문 substring 검색보다 형태소·품사 필터가 알맞다.

## 금칙어 탐지와 마스킹

```kotlin
import io.bluetape4k.tokenizer.model.blockwordOptionsOf
import io.bluetape4k.tokenizer.model.blockwordRequestOf
import java.util.Locale

val options = blockwordOptionsOf(locale = Locale.JAPANESE)
val response = JapaneseProcessor.maskBlockwords(
    blockwordRequestOf("ホモの男性を理解できない", options),
)

println(response.maskedText)      // **の男性を理解できない
println(response.blockwordExists) // true
```

내장 경로는 명사와 동사를 중심으로 찾고, 단일 토큰이 일치하지 않으면 인접한 명사와 명사·동사 조합도 확인한다.
`Locale.JAPANESE`가 필요합니다. severity는 cumulative threshold로 동작해 `LOW`는 모든 tier, `MIDDLE`은 middle/high, `HIGH`는 high tier만 검사합니다.

## 사전 수명

패키지 사전은 금칙어 기능을 처음 사용할 때 지연 로드된다. `addBlockwords`, `removeBlockwords`, `clearBlockwords`는 프로세스 전체의 메모리 정책을 바꾼다. 첫 호출 지연이 중요하면 미리 warmup하고, 애플리케이션 업데이트는 재시작 뒤 복구한다.

## 선택 기준

일본어 토큰 경계나 품사가 필요할 때 선택한다. 형태소와 무관한 정확한 패턴은 [텍스트 검색](text-search.md)을 사용한다. 입력 언어가 정해지지 않았을 때만 [Lingua](lingua.md)로 앞에서 분기한다.

## 다음 학습 경로

- [혼합 언어 처리](../guides/mixed-language-processing.md)
- [토크나이저 안전 예제](../examples/tokenizer-safety-examples.md)
- [시작과 메모리](../operations/startup-and-memory.md)

## 소스 근거

- [JapaneseProcessor](https://github.com/bluetape4k/bluetape4k-text/blob/0.3.0/tokenizer-japanese/src/main/kotlin/io/bluetape4k/tokenizer/japanese/JapaneseProcessor.kt)
- [일본어 모듈 README](https://github.com/bluetape4k/bluetape4k-text/blob/0.3.0/tokenizer-japanese/README.md)
- [일본어 프로세서 테스트](https://github.com/bluetape4k/bluetape4k-text/blob/0.3.0/tokenizer-japanese/src/test/kotlin/io/bluetape4k/tokenizer/japanese/JapaneseProcessorTest.kt)

<!-- release-readme-diagrams:start -->
## 배포본 다이어그램 {#release-diagrams}

아래 그림은 `0.3.0` 배포본의 README 자산을 해당 배포 커밋에서 직접 불러옵니다. 이후 SNAPSHOT이 아니라 이 매뉴얼 버전의 구조와 실행 흐름을 보여 줍니다. 미리보기를 누르면 같은 배포 커밋의 SVG 원본이 열립니다.

### tokenizer japanese 클래스 구조도

[![tokenizer japanese 클래스 구조도](https://raw.githubusercontent.com/bluetape4k/bluetape4k-text/aead213d2d25307d7d3684226943a5f95c7411f2/docs/images/readme-diagrams/tokenizer-japanese-class-01.png)](https://github.com/bluetape4k/bluetape4k-text/blob/aead213d2d25307d7d3684226943a5f95c7411f2/docs/images/readme-diagrams/tokenizer-japanese-class-01.svg)

_배포본 README: [`tokenizer-japanese/README.ko.md`](https://github.com/bluetape4k/bluetape4k-text/blob/aead213d2d25307d7d3684226943a5f95c7411f2/tokenizer-japanese/README.ko.md)_

<!-- release-readme-diagrams:end -->
