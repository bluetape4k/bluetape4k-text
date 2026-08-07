# 사전과 금칙어

사전은 오래 유지되는 정책 데이터다. 라이브러리는 loader와 런타임 변경 API를 제공하지만 소유권, 검증, 저장, 배포 방식은 애플리케이션에서 정해야 한다.

## 패키지 리소스 읽기

`DictionaryProvider`는 classpath 리소스를 지연 sequence, 빈도 map, 통합 `CharArraySet`으로 읽는다. 일반 텍스트와 gzip 파일을 지원한다.

```kotlin
import io.bluetape4k.tokenizer.utils.DictionaryProvider

val blocked = DictionaryProvider.readWords(
    "dictionary/base.txt",
    "dictionary/product-overrides.txt",
)

if ("restricted" in blocked) {
    // 애플리케이션 정책 적용
}
```

여러 리소스는 비동기 Flow 경로로 읽는다. 초기화 중에 실행하고 필요한 정책을 읽지 못하면 시작을 실패시키며 결과는 재사용한다.

## 한국어 런타임 업데이트

```kotlin
import io.bluetape4k.tokenizer.korean.KoreanProcessor
import io.bluetape4k.tokenizer.model.Severity

KoreanProcessor.addNounsToDictionary("블루테이프4K")
KoreanProcessor.addBlockwords(listOf("금칙어"), Severity.MIDDLE)
```

추가 명사는 이후 형태소 분석에 영향을 주고, 금칙어는 등급별로 관리된다. 호출 정책이 어느 등급까지 적용하는지 정하고 실제 마스킹 결과를 테스트한다.

## 일본어 런타임 업데이트

```kotlin
import io.bluetape4k.tokenizer.japanese.JapaneseProcessor

JapaneseProcessor.addBlockwords(listOf("東京"))
JapaneseProcessor.removeBlockwords(listOf("東京"))
```

일본어 패키지 사전은 지연 로드된다. 탐지는 명사와 동사를 중심으로 하며 인접 토큰을 복합어로 합쳐 검사할 수 있다.

## 정책 소유권

안전한 업데이트 절차는 다음 질문에 답해야 한다.

- 누가 어떤 단어와 등급을 배포할 수 있는가?
- Unicode 정규화와 빈 줄을 어떻게 처리하는가?
- 중복·충돌 항목을 어떻게 해결하는가?
- 모든 인스턴스가 같은 버전을 어떻게 받는가?
- 재시작 뒤 런타임 업데이트를 어떻게 복구하는가?
- 잘못된 배포를 어떻게 되돌리는가?

메모리 변경 API는 분산 문제를 해결하지 않는다. 이미 검증한 정책 snapshot을 설치하는 지점으로 사용한다.

## 마스킹은 권한 판정이 아니다

마스킹 결과는 화면에 보여 줄 문자열이다. 신뢰할 수 있는 결정 계층에서는 일치한 정책 분류와 단어 목록을 유지할 수 있지만 오류나 로그에 사용자 원문을 노출해서는 안 된다. 겹침, 활용형, 복합어에 형태소 분석과 정확한 automaton 중 무엇이 필요한지도 정한다.

## 테스트

등급별·언어별 작은 fixture, 일본어 복합어, 한국어 런타임 명사, 빈 사전, 재시작·재로딩을 검증한다. 업데이트 순서 때문에 인스턴스마다 정책이 달라지지 않는지도 확인한다.

[테스트](testing.md), [입력 안전성](input-safety.md), [품질 보고서](../quality/quality-gates.md)로 이어서 학습하자.

## 소스 근거

- [DictionaryProvider](https://github.com/bluetape4k/bluetape4k-text/blob/0.3.0/tokenizer-core/src/main/kotlin/io/bluetape4k/tokenizer/utils/DictionaryProvider.kt)
- [KoreanProcessor](https://github.com/bluetape4k/bluetape4k-text/blob/0.3.0/tokenizer-korean/src/main/kotlin/io/bluetape4k/tokenizer/korean/KoreanProcessor.kt)
- [JapaneseProcessor](https://github.com/bluetape4k/bluetape4k-text/blob/0.3.0/tokenizer-japanese/src/main/kotlin/io/bluetape4k/tokenizer/japanese/JapaneseProcessor.kt)
