# 한국어 토크나이저 라이브러리

`tokenizer-korean`은 한국어 처리 기능을 한데 모은 모듈이다. `KoreanProcessor` facade에서 구어체 정규화, 형태소 분석, 어간·구문 추출, 문장 분리, detokenization, 런타임 명사 추가, 금칙어 마스킹을 제공한다.

## 제공하는 기능

- 반복 문자와 알려진 구어체를 다듬는 정규화
- 26개 품사 체계의 1-best·top-N 토큰화
- 명사 중심 토큰화와 구문 추출
- 동사·형용사 어간 복원
- 문장 분리와 detokenization
- URL, email, hashtag, screen name, 숫자, 한국어, 영문, 문장 부호 chunk
- 등급별 금칙어 사전과 런타임 명사 확장

## 의존성 추가하기

```kotlin
dependencies {
    implementation("io.github.bluetape4k.text:tokenizer-korean:0.3.0")
}
```

## 가장 작은 예제

```kotlin
import io.bluetape4k.tokenizer.korean.KoreanProcessor

val normalized = KoreanProcessor.normalize("안됔ㅋㅋㅋㅋㅋ")
val tokens = KoreanProcessor.tokenize("주말특가 쇼핑몰")

println(normalized)                              // 안돼ㅋㅋㅋ
println(KoreanProcessor.tokensToStrings(tokens)) // [주말, 특가, 쇼핑몰]
```

뒤 단계에서 품사, 위치, 어간을 쓴다면 `KoreanToken`을 그대로 유지한다. `tokensToStrings`는 표시나 간단한 검증에 편하지만 분석 결과 전체를 대신하지 않는다.

## 처리 기능 조합하기

```kotlin
val stemmed = KoreanProcessor.stem(KoreanProcessor.tokenize("가느다란"))
println(stemmed.first().stem) // 갈다

val phrases = KoreanProcessor.extractPhrases(
    KoreanProcessor.tokenize("성탄절 쇼핑"),
    filterSpam = false,
)

val sentences = KoreanProcessor.splitSentences("안녕? 세상아?").toList()
```

정규화, 토큰화, 어간 추출, 구문 추출은 별도 연산이다. 결과 계약에 필요한 단계만 적용하고, 문자열로 너무 일찍 줄여 정보를 잃지 않도록 한다.

## 런타임 사전과 마스킹

```kotlin
import io.bluetape4k.tokenizer.model.BlockwordRequest
import io.bluetape4k.tokenizer.model.Severity

KoreanProcessor.addNounsToDictionary("블루테이프4K", "주말특가")
KoreanProcessor.addBlockwords(listOf("욕설"), Severity.HIGH)

val response = KoreanProcessor.maskBlockwords(BlockwordRequest("이 욕설은 나쁜 말이야"))
println(response.maskedText) // 이 **은 나쁜 말이야
```

변경 사항은 같은 프로세스의 이후 결과에 영향을 준다. 여러 인스턴스가 같은 정책을 써야 한다면 업데이트를 인증·저장·배포해야 한다.

## 선택 기준

한국어 형태소 분석이나 내장 정규화·구문 기능이 필요할 때 선택한다. 정확한 패턴만 여러 개 찾는다면 [텍스트 검색](text-search.md)이 알맞다. 입력 언어를 모르는 경우에만 [Lingua](lingua.md)를 앞단에 둔다.

## 제약과 실패

facade는 core의 최대 입력 길이를 검사하며 동시 호출에 안전하다. 다만 런타임 사전 변경은 공유 정책을 바꾼다. 지연 시간에 민감한 서비스는 사용할 연산을 미리 호출하고, 실패한 입력 원문은 로그에 남기지 않는다.

## 다음 학습 경로

- [혼합 언어 처리](../guides/mixed-language-processing.md)
- [사전과 금칙어](../guides/dictionaries-and-blockwords.md)
- [테스트](../guides/testing.md)

## 소스 근거

- [KoreanProcessor](https://github.com/bluetape4k/bluetape4k-text/blob/0.3.0/tokenizer-korean/src/main/kotlin/io/bluetape4k/tokenizer/korean/KoreanProcessor.kt)
- [한국어 모듈 README](https://github.com/bluetape4k/bluetape4k-text/blob/0.3.0/tokenizer-korean/README.md)
- [한국어 프로세서 테스트](https://github.com/bluetape4k/bluetape4k-text/blob/0.3.0/tokenizer-korean/src/test/kotlin/io/bluetape4k/tokenizer/korean/KoreanProcessorTest.kt)

<!-- release-readme-diagrams:start -->
## 배포본 다이어그램 {#release-diagrams}

아래 그림은 `0.3.0` 배포본의 README 자산을 해당 배포 커밋에서 직접 불러옵니다. 이후 SNAPSHOT이 아니라 이 매뉴얼 버전의 구조와 실행 흐름을 보여 줍니다. 미리보기를 누르면 같은 배포 커밋의 SVG 원본이 열립니다.

### tokenizer korean 클래스 구조도

[![tokenizer korean 클래스 구조도](https://raw.githubusercontent.com/bluetape4k/bluetape4k-text/aead213d2d25307d7d3684226943a5f95c7411f2/docs/images/readme-diagrams/tokenizer-korean-class-01.png)](https://github.com/bluetape4k/bluetape4k-text/blob/aead213d2d25307d7d3684226943a5f95c7411f2/docs/images/readme-diagrams/tokenizer-korean-class-01.svg)

_배포본 README: [`tokenizer-korean/README.ko.md`](https://github.com/bluetape4k/bluetape4k-text/blob/aead213d2d25307d7d3684226943a5f95c7411f2/tokenizer-korean/README.ko.md)_

<!-- release-readme-diagrams:end -->
