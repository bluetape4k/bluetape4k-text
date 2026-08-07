# 언어 감지 라이브러리

`lingua`는 Lingua 감지기를 Kotlin답게 구성하는 API와 문자 범위를 살피는 Unicode 도우미를 제공한다. 한 언어를 추정하거나 혼합 텍스트의 후보 언어를 모으고, 통계 모델을 쓰기 전에 빠른 문자 범위 분기를 만들 때 사용한다.

## 제공하는 기능

- 전체 언어, `Language` 집합, ISO 코드 집합을 받는 감지기 factory
- builder와 parameter 방식 설정
- 혼합 입력을 위한 `detectAllLanguagesOf(text): Set<Language>`
- 짧은 Latin token의 모호한 결과 보정
- 한국어·일본어·중국어·태국어·Latin 문자를 찾는 `UnicodeDetector`와 문자 확장

## 의존성 추가하기

```kotlin
dependencies {
    implementation("io.github.bluetape4k.text:lingua:0.3.0")
}
```

상위 Lingua 엔진은 전이 의존성으로 들어온다.

## 가장 작은 예제

```kotlin
import com.github.pemistahl.lingua.api.Language
import io.bluetape4k.lingua.detectAllLanguagesOf
import io.bluetape4k.lingua.languageDetectorOf

val detector = languageDetectorOf(
    languages = setOf(Language.ENGLISH, Language.KOREAN, Language.JAPANESE),
    minimumRelativeDistance = 0.0,
    isEveryLanguageModelPreloaded = true,
    isLowAccuracyModeEnabled = false,
)

println(detector.detectAllLanguagesOf("Hello 안녕하세요 こんにちは"))
```

애플리케이션이 지원하는 언어만 넣어 감지기를 한 번 만들고 재사용한다. 모델을 미리 로드하면 시작 시간과 메모리를 쓰는 대신 이후 호출을 예측하기 쉬워진다.

## 문자 범위로 먼저 분기하기

```kotlin
import io.bluetape4k.lingua.UnicodeDetector
import java.util.Locale

val unicode = UnicodeDetector()
println(unicode.containsAny("Hello 안녕", Locale.KOREAN)) // true
println(unicode.filterString("Hello 안녕", Locale.KOREAN)) // [안, 녕]
```

Unicode 필터는 문자 범위를 결정적으로 판정하지만 자연어를 추론하지는 않는다. 지원 문자 여부만 빠르게 확인할 때 적합하고, 라틴 언어 구분이나 모호한 혼합 텍스트에는 통계 감지기를 사용한다.

## 선택 기준

지원 언어가 정해져 있다면 제한된 감지기를 만든다. 정말 넓은 언어 집합이 필요한 경우에만 전체 감지기를 사용한다. 저정확도 모드는 자원과 품질을 바꾸는 명시적인 선택이므로 실제 입력 corpus로 검증해야 한다.

## 제약과 실패

감지 결과는 근거이지 확정 사실이 아니다. 짧은 입력과 공통 단어는 모호할 수 있다. 빈 혼합 입력은 빈 집합을 반환하고 token별 결과가 없으면 전체 텍스트 감지로 돌아간다. 알 수 없거나 모호한 결과를 위한 경로를 따로 둔다.

## 다음 학습 경로

- [혼합 언어 처리](../guides/mixed-language-processing.md)
- [Lingua 실행 예제](../examples/lingua-examples.md)
- [런타임 경계](../architecture/runtime-boundaries.md)

## 소스 근거

- [감지기 factory](https://github.com/bluetape4k/bluetape4k-text/blob/0.3.0/lingua/src/main/kotlin/io/bluetape4k/lingua/LanguageDetector.kt)
- [UnicodeDetector](https://github.com/bluetape4k/bluetape4k-text/blob/0.3.0/lingua/src/main/kotlin/io/bluetape4k/lingua/UnicodeDetector.kt)
- [Lingua 예제](https://github.com/bluetape4k/bluetape4k-text/blob/0.3.0/examples/lingua-examples/src/main/kotlin/io/bluetape4k/text/examples/lingua/LinguaExamples.kt)

<!-- release-readme-diagrams:start -->
## 배포본 다이어그램 {#release-diagrams}

아래 그림은 `0.3.0` 배포본의 README 자산을 해당 배포 커밋에서 직접 불러옵니다. 이후 SNAPSHOT이 아니라 이 매뉴얼 버전의 구조와 실행 흐름을 보여 줍니다. 미리보기를 누르면 같은 배포 커밋의 SVG 원본이 열립니다.

### lingua 아키텍처

[![lingua 아키텍처](https://raw.githubusercontent.com/bluetape4k/bluetape4k-text/aead213d2d25307d7d3684226943a5f95c7411f2/docs/images/readme-diagrams/lingua-architecture-01.png)](https://github.com/bluetape4k/bluetape4k-text/blob/aead213d2d25307d7d3684226943a5f95c7411f2/docs/images/readme-diagrams/lingua-architecture-01.svg)

_배포본 README: [`lingua/README.ko.md`](https://github.com/bluetape4k/bluetape4k-text/blob/aead213d2d25307d7d3684226943a5f95c7411f2/lingua/README.ko.md)_

### lingua 클래스 구조도

[![lingua 클래스 구조도](https://raw.githubusercontent.com/bluetape4k/bluetape4k-text/aead213d2d25307d7d3684226943a5f95c7411f2/docs/images/readme-diagrams/lingua-class-02.png)](https://github.com/bluetape4k/bluetape4k-text/blob/aead213d2d25307d7d3684226943a5f95c7411f2/docs/images/readme-diagrams/lingua-class-02.svg)

_배포본 README: [`lingua/README.ko.md`](https://github.com/bluetape4k/bluetape4k-text/blob/aead213d2d25307d7d3684226943a5f95c7411f2/lingua/README.ko.md)_

<!-- release-readme-diagrams:end -->
