# 시작하기

의존성을 설정하고 첫 토큰화 결과를 확인해 보자. 결과를 눈으로 확인하기 쉬운 한국어 프로세서를 사용하지만 버전 관리 원칙은 모든 Text 모듈에 똑같이 적용된다.

## 준비 사항

- JDK 25 이상
- Kotlin 2.4와 호환되는 빌드
- Maven Central을 사용하는 저장소 설정

## 생태계 BOM을 먼저 사용하기

여러 bluetape4k 저장소의 라이브러리를 함께 쓴다면 `bluetape4k-dependencies`를 가져오는 편이 좋다. 사용자는 보통 이 BOM 버전만 정하면 되고, Text를 비롯한 Kotlin 생태계의 호환 버전은 BOM이 맞춘다.

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<release>"))
    implementation("io.github.bluetape4k.text:tokenizer-korean")
}
```

Text 라이브러리만 사용하거나 전체 생태계 버전을 애플리케이션에서 따로 관리한다면 Text 전용 BOM을 가져올 수 있다.

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k.text:bluetape4k-text-bom:0.3.0"))
    implementation("io.github.bluetape4k.text:tokenizer-korean")
}
```

BOM은 버전 제약만 제공한다. 토크나이저 클래스가 필요하다면 위 예제처럼 실제 런타임 모듈도 추가해야 한다.

## 가장 작은 예제 실행하기

```kotlin
import io.bluetape4k.tokenizer.korean.KoreanProcessor

fun main() {
    val normalized = KoreanProcessor.normalize("안됔ㅋㅋㅋㅋㅋ")
    val tokens = KoreanProcessor.tokenize("주말특가 쇼핑몰")

    println(normalized)
    println(KoreanProcessor.tokensToStrings(tokens))
}
```

실행 결과는 다음과 같다.

```text
안돼ㅋㅋㅋ
[주말, 특가, 쇼핑몰]
```

`normalize`는 알려진 구어체를 교정하고 반복되는 웃음 문자를 제한한다. `tokenize`는 형태소를 분석한다. `tokensToStrings`는 토큰 문자열만 보기 좋게 꺼내는 도우미이므로 품사나 위치, 어간이 필요하다면 원래 토큰 객체를 유지해야 한다.

## 다음 모듈 고르기

```kotlin
dependencies {
    implementation("io.github.bluetape4k.text:tokenizer-japanese")
    implementation("io.github.bluetape4k.text:lingua")
    implementation("io.github.bluetape4k.text:text-search")
}
```

서비스에서 실제로 쓰는 모듈만 추가하자. 언어 감지기는 텍스트를 토큰화하지 않고, 토크나이저는 여러 키워드를 한 번에 찾는 검색기를 대신하지 않는다. [처리 모델](architecture/processing-model.md)은 이 기능들을 불필요하게 한 파이프라인으로 묶지 않고 조합하는 방법을 설명한다.

## 요청을 받기 전에

신뢰할 수 없는 HTTP 입력을 프로세서에 바로 넘기면 안 된다. `tokenizer-core` 요청 모델로 빈 문자열과 지나치게 긴 텍스트를 먼저 검사하고, 오류 응답에는 제출된 본문을 포함하지 않는다. 자세한 내용은 [입력 안전성](guides/input-safety.md)과 [실행 가능한 안전 예제](examples/tokenizer-safety-examples.md)를 참고하자.

언어 감지기와 불변 검색 automaton도 요청마다 다시 만들지 말고 재사용한다. [시작과 메모리](operations/startup-and-memory.md)에서 수명 관리 방법을 확인할 수 있다.

## 소스 근거

- [KoreanProcessor facade](https://github.com/bluetape4k/bluetape4k-text/blob/0.3.0/tokenizer-korean/src/main/kotlin/io/bluetape4k/tokenizer/korean/KoreanProcessor.kt)
- [Text BOM README](https://github.com/bluetape4k/bluetape4k-text/blob/0.3.0/bom/README.md)
- [루트 설치 예제](https://github.com/bluetape4k/bluetape4k-text/blob/0.3.0/README.md)
