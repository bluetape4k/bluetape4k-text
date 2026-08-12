한국어 | [English](./README.md)

# tokenizer-korean

한국어 형태소 분석, 정규화, 구문 추출, 어간 복원, 문장 분리, 금칙어 마스킹을 제공하는 NLP 라이브러리입니다. `twitter-text` 의존성 없이 동작하며, URL/Hashtag/Mention/CashTag 패턴은 `TwitterCompatPatterns.kt`에서 내부적으로 구현됩니다.

## 아키텍처

![tokenizer korean Class Structure diagram](../docs/images/readme-diagrams/tokenizer-korean-class-01.png)

## 주요 기능

- **텍스트 정규화** — 구어체 반복 문자 축약 (`ㅋㅋㅋㅋ → ㅋㅋㅋ`), 오타 교정 (`가쟝 → 가장`), 받침 `ㄴ` 탈락 보정 (`버슨가 → 버스인가`)
- **형태소 분석** — 1-best 및 상위 N개 형태소 분석, 26종 품사 태깅
- **명사 중심 토크나이저** — 구문 추출에 최적화된 경량 토크나이저 경로
- **구문 추출** — 전체 토큰 또는 명사 중심 토큰 스트림에서 명사 구문 추출, 해시태그 구문 지원
- **어간 복원 (Stemming)** — 용언 활용형을 원형으로 복원 (`가느다란 → stem: 갈다`)
- **문장 분리** — 텍스트를 `Sequence<Sentence>`로 분리
- **역토크나이징** — 토큰 목록을 자연스러운 문장으로 복원
- **금칙어 마스킹** — `LOW`/`MIDDLE`/`HIGH` 심각도 계층 사전 관리 및 `**` 마스킹
- **청킹** — 전처리 품사 패턴 기반 청킹: URL, Email, Hashtag, ScreenName, CashTag, Number, Korean, Alpha, Punctuation
- **twitter-text 의존성 제거** — URL/Hashtag/Mention/CashTag 정규식 패턴이 `TwitterCompatPatterns.kt`에 내부 구현
- **런타임 사전 업데이트** — 재시작 없이 명사 또는 금칙어를 런타임에 추가
- **suspend 사전 preload** — startup/readiness에서 `KoreanProcessor.preload()`를 호출하며, 직접 동기 조회는 호환성 fallback으로 유지
- **스레드 안전** — `KoreanProcessor`의 모든 메서드는 동시 사용에 안전

## 사용법

### 시작 단계 사전 preload

애플리케이션 startup coroutine에서 facade의 preload를 호출하면 첫 사전 기반
연산이 요청 스레드에서 리소스 I/O를 수행하지 않습니다.

```kotlin
import io.bluetape4k.tokenizer.korean.KoreanProcessor

suspend fun warmUpKoreanTokenizer() {
    KoreanProcessor.preload()
}
```

```kotlin
import io.bluetape4k.tokenizer.korean.KoreanProcessor
import io.bluetape4k.tokenizer.korean.tokenizer.TokenizerProfile
import io.bluetape4k.tokenizer.model.BlockwordRequest
import io.bluetape4k.tokenizer.model.Severity

// 1. 구어체 텍스트 정규화
val normalized = KoreanProcessor.normalize("안됔ㅋㅋㅋㅋㅋ")
// → "안돼ㅋㅋㅋ"

// 2. 형태소 분석
val tokens = KoreanProcessor.tokenize("주말특가 쇼핑몰")
KoreanProcessor.tokensToStrings(tokens)
// ["주말", "특가", "쇼핑몰"]

// 3. 어간 복원
val stemmed = KoreanProcessor.stem(KoreanProcessor.tokenize("가느다란"))
println(stemmed.first().stem)  // → "갈다"

// 4. 구문 추출
val phrases = KoreanProcessor.extractPhrases(
    KoreanProcessor.tokenize("성탄절 쇼핑"),
    filterSpam = false
)
phrases.forEach { println(it.text) }

// 5. 문장 분리
val sentences = KoreanProcessor.splitSentences("안녕? 세상아?").toList()
// size == 2

// 6. 역토크나이징
val text = KoreanProcessor.detokenize(listOf("뭐", "완벽", "하진", "않", "지만"))
// → "뭐 완벽하진 않지만"

// 7. 런타임 명사 사전 확장
KoreanProcessor.addNounsToDictionary("블루테이프4K", "주말특가")

// 8. 금칙어 마스킹
KoreanProcessor.addBlockwords(listOf("욕설"), Severity.HIGH)
val response = KoreanProcessor.maskBlockwords(BlockwordRequest("이 욕설은 나쁜 말이야"))
// response.text → "이 **은 나쁜 말이야"

// 9. 상위 N개 분석 후보
val topN = KoreanProcessor.tokenizeTopN("대학", n = 2)

// 10. 코루틴으로 병렬 토크나이징
runBlocking(Dispatchers.Default) {
    listOf("텍스트1", "텍스트2", "텍스트3").map { text ->
        async { KoreanProcessor.tokenize(text) }
    }.awaitAll()
}
```

## 의존성

| 의존성 | 목적 |
|---|---|
| `tokenizer-core` | `BlockwordRequest`, `Severity`, 토크나이저 계약 인터페이스 |
| `bluetape4k-coroutines` | 코루틴 기반 청킹 파이프라인 |
| `bluetape4k-io` | I/O 유틸리티 |
| `eclipse-collections` | 사전 저장을 위한 고성능 컬렉션 |
| `commons-collections4` | 컬렉션 유틸리티 |

```kotlin
// build.gradle.kts
implementation("io.github.bluetape4k.text:tokenizer-korean:<current release or snapshot>")
```

## 웹 서비스 입력 경계

HTTP boundary에서는 `KoreanProcessor` 호출 전에 `tokenizeRequestOf`와
`blockwordRequestOf`로 입력을 검증하세요. blank 입력은 `400 Bad Request`,
`MAX_TOKENIZE_TEXT_LENGTH` 또는 `MAX_BLOCKWORD_TEXT_LENGTH`를 초과한 입력은
`413 Payload Too Large`로 매핑하는 것이 좋습니다. 오류 응답에는 status와 길이
metadata만 담고 제출된 텍스트는 포함하지 마세요.

실행 가능한 한국어/일본어 safety sample은
[`../examples/tokenizer-safety-examples`](../examples/tokenizer-safety-examples)를 참고하세요.
