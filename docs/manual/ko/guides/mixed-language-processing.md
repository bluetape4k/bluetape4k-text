# 혼합 언어 처리

혼합 텍스트를 다루려면 명시적인 라우팅 정책이 필요하다. 감지된 언어 집합은 그 정책을 위한 근거이지 모든 프로세서를 호출하라는 명령이 아니다.

![언어 감지 선택 흐름](../../assets/guides/language-detection-selection.png)

## 서비스 계약부터 확인하기

endpoint가 이미 언어별로 나뉘었다면 입력을 검증하고 해당 프로세서를 바로 호출한다. 감지를 추가해도 라우팅은 달라지지 않고 비용과 모호함만 늘어난다. 입력 언어를 모르거나 여러 프로세서를 지원하거나 제품에 언어 메타데이터가 필요할 때 감지기를 사용한다.

## 문자 범위로 빠르게 분기하기

```kotlin
import io.bluetape4k.lingua.UnicodeDetector
import java.util.Locale

val unicode = UnicodeDetector()
val hasKorean = unicode.containsAny("Hello 안녕하세요", Locale.KOREAN)
val hasJapanese = unicode.containsAny("Hello こんにちは", Locale.JAPANESE)
```

이 코드는 문자 범위를 찾을 뿐 자연어를 판별하지 않는다. 좁은 질문에는 결정적이고 빠른 근거가 된다.

## 지원 언어만 넣은 감지기 만들기

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

val detected = detector.detectAllLanguagesOf("Hello. 안녕하세요. こんにちは。")
```

지원 언어 집합을 제한하면 제품 정책이 코드에 드러나고 불필요한 모델 범위도 줄어든다. 감지기는 재사용한다.

## 모호한 결과 처리하기

| 근거 | 권장 동작 |
|---|---|
| 한국어만 발견 | `KoreanProcessor` 호출 |
| 일본어만 발견 | `JapaneseProcessor` 호출 |
| 한국어와 일본어 발견 | 제품에 안전한 분리 규칙이 있을 때만 나누고, 아니면 혼합·미지원으로 처리 |
| Latin 언어만 발견 | 메타데이터로 유지하거나 다른 저장소의 프로세서 사용 |
| 빈 결과 또는 모호함 | unknown 반환, 언어 hint 요청, 문서화된 fallback 적용 |

offset과 책임을 명확히 유지할 수 없다면 여러 프로세서 결과를 단순히 이어 붙여 하나의 분석으로 만들지 않는다.

## 원문과 위치 보존하기

문자 범위로 뽑은 substring은 근거로 유용하지만 전체 문맥을 잃는다. 신뢰할 수 있는 처리에는 원문을 유지하되 오류 로그에는 남기지 않는다. 정확한 키워드 위치가 필요하면 원문에 `text-search`를 적용하거나 분리 뒤 mapping을 명시적으로 관리한다.

## 실제 혼합 입력 테스트하기

문장 부호, 숫자, 짧은 Latin token, 한국어·일본어 문장, 언어가 없는 입력을 포함한다. 감지 집합과 애플리케이션 라우팅을 따로 검증하면 감지기 개선이 프로세서 동작을 몰래 바꾸는 일을 막을 수 있다.

[Lingua 예제](../examples/lingua-examples.md), [런타임 경계](../architecture/runtime-boundaries.md), [실패 계약](../operations/failure-contracts.md)으로 이어서 살펴보자.
