# 보안 발견 사항 — bluetape4k-text

**날짜:** 2026-05-17
**합계:** Critical 0건, High 0건, Medium 3건, Low 1건

---

## [MEDIUM] 발견 1: 예외 메시지가 사용자 입력 텍스트를 포함함 — PII 노출

- **OWASP:** A09 — Security Logging and Monitoring Failures
- **STRIDE:** Information Disclosure
- **신뢰도:** 확인됨
- **위치:**
  - `tokenizer-korean/src/main/.../block/KoreanBlockwordProcessor.kt:84`
  - `tokenizer-korean/src/main/.../block/KoreanBlockwordProcessor.kt:137`
  - `tokenizer-japanese/src/main/.../block/JapaneseBlockwordProcessor.kt:142`

### 코드 증거

```kotlin
// KoreanBlockwordProcessor.kt:84
throw TokenizerException("Fail to mask block word. text=[$text]", e)

// KoreanBlockwordProcessor.kt:137
throw TokenizerException("Fail to mask block word. request=$request", e)

// JapaneseBlockwordProcessor.kt:142
throw TokenizerException("Fail to mask block word. request=$request", e)
```

`BlockwordRequest.toString()`은 전체 `text` 필드를 포함한다. 이 라이브러리를 웹 서비스 안에서 사용할 경우 다음 문제가 생긴다.

1. 전체 사용자 입력이 예외 메시지에 들어가 서버 로그로 전파된다.
2. 예외를 잡아 HTTP 응답으로 다시 던지면 사용자 입력이 오류 본문에 노출된다.
3. 텍스트에 PII(실명, 의료 기록 등)가 포함되면 로그가 PII 저장소가 된다.

### 공격 시나리오

1. 웹 서비스가 `{"text": "사용자_실명_주민번호_X"}` 본문을 가진 `POST /check-text`를 받는다.
2. 내부 토크나이저 오류가 발생한다.
3. `TokenizerException("Fail to mask block word. text=[사용자_실명_주민번호_X]")`가 던져진다.
4. 예외가 HTTP 500 응답이나 log aggregator로 전파된다.
5. PII가 로그와 잠재적인 오류 응답에서 보이게 된다.

### 완화책

```kotlin
// Before
throw TokenizerException("Fail to mask block word. text=[$text]", e)

// After — text 본문은 생략하고 구조적 metadata만 포함한다.
throw TokenizerException(
    "Fail to mask block word. textLength=${text.length}, locale=${request.options.locale}",
    e
)
```

---

## [MEDIUM] 발견 2: 최대 입력 길이 검증 부재 — 웹 서비스 wrapper의 DoS 위험

- **OWASP:** A04 — Insecure Design
- **STRIDE:** Denial of Service
- **신뢰도:** 확인됨
- **위치:**
  - `tokenizer-korean/.../KoreanProcessor.kt` 전체 public 함수
  - `tokenizer-japanese/.../JapaneseProcessor.kt` 전체 public 함수
  - `tokenizer-core/.../model/TokenizeRequest.kt`

### 코드 증거

```kotlin
// tokenizeRequestOf — no length validation
fun tokenizeRequestOf(
    text: String,               // accepts arbitrarily long input
    options: TokenizeOptions = TokenizeOptions.DEFAULT,
): TokenizeRequest {
    text.requireNotBlank("text")  // only validates non-blank, not length
    return TokenizeRequest(text, options)
}
```

`KoreanChunker`의 O(n²) 복잡도 버그(#42)와 결합되면 100,000자 입력 하나가 요청당 수 GB 메모리와 수 초의 CPU를 소비할 수 있다. public API 어디에도 최대 입력 길이가 강제되지 않는다.

### 영향

라이브러리 자체는 웹 서버가 아니므로 이는 **라이브러리 수준 위험**이다. 그러나 별도 입력 검증 없이 웹 서비스로 감싸면 실제 DoS 취약점이 된다. 이 라이브러리 위에 REST API를 만드는 Maven Central 소비자는 이 위험을 상속한다.

### 완화책

```kotlin
private const val MAX_TEXT_LENGTH = 100_000  // or configurable

fun tokenizeRequestOf(text: String, options: TokenizeOptions = TokenizeOptions.DEFAULT): TokenizeRequest {
    text.requireNotBlank("text")
    require(text.length <= MAX_TEXT_LENGTH) {
        "text too long: ${text.length} chars (max $MAX_TEXT_LENGTH)"
    }
    return TokenizeRequest(text, options)
}
```

대안으로 KDoc에 입력 길이 검증이 없음을 문서화해 소비자가 직접 검증해야 함을 알린다.

---

## [MEDIUM] 발견 3: 20개 이상 Serializable class에 serialVersionUID 누락 — 역직렬화 무결성

- **OWASP:** A08 — Software and Data Integrity Failures
- **STRIDE:** Tampering (integrity)
- **신뢰도:** 확인됨
- **참고:** 이미 issue #27로 추적 중

### serialVersionUID가 누락된 class

| Class | Module |
|---|---|
| `AbstractMessage` | tokenizer-core |
| `BlockwordOptions` | tokenizer-core |
| `TokenizeOptions` | tokenizer-core |
| `CharArrayMap` | tokenizer-core |
| `CharArraySet` | tokenizer-core |
| `CharacterUtils` | tokenizer-core |
| `KoreanToken` | tokenizer-korean |
| `KoreanChunk` | tokenizer-korean |
| `KoreanPhrase` | tokenizer-korean |
| `CandidateParse` | tokenizer-korean |
| `ParsedChunk` | tokenizer-korean |
| `PossibleTrie` | tokenizer-korean |
| `Sentence` | tokenizer-korean |
| `TokenizerProfile` | tokenizer-korean |
| `KoreanPosTrie` | tokenizer-korean |
| `PhraseBuffer` | tokenizer-korean |
| `KoreanPos` (enum) | tokenizer-korean |
| `Hangul.HangulChar` | tokenizer-korean |
| `InternalToken` | text-search |
| `InternalTrieConfig` | text-search |

명시적인 `serialVersionUID`가 없으면 JVM은 class 구조에서 값을 자동 생성한다. 필드 추가, 제거, 이름 변경이 발생하면 UID가 달라져 기존에 저장된 instance(cache, distributed session, message queue)를 역직렬화할 때 `InvalidClassException`이 발생한다.

### 완화책

모든 `Serializable` class에 다음을 추가한다.

```kotlin
companion object {
    private const val serialVersionUID = 1L
}
```

---

## [LOW] 발견 4: DictionaryProvider 오류 메시지의 path — 내부 경로 노출

- **OWASP:** A05 — Security Misconfiguration
- **STRIDE:** Information Disclosure
- **신뢰도:** 확인됨
- **위치:** `tokenizer-core/src/main/.../utils/DictionaryProvider.kt:70`

### 코드 증거

```kotlin
val stream: InputStream? = classLoader.getResourceAsStream(path)
check(stream != null) { "Can't open file. path=$path" }
```

`path`가 소비자 제어 값이고 resource가 없으면 오류 메시지가 전체 classpath resource path를 노출한다. 라이브러리에는 웹 노출 API가 없으므로 위험은 낮지만, 예외가 client로 전파되면 내부 project 구조를 드러낼 수 있다.

### 완화책

```kotlin
check(stream != null) { "Dictionary resource not found: $path" }
// 또는 path가 민감한 directory 구조를 포함할 수 있으면 path를 생략한다.
```

---

## N/A 항목

| OWASP Category | 판정 | 이유 |
|---|---|---|
| A01 Broken Access Control | N/A | 접근 제어 없음, 순수 library |
| A02 Cryptographic Failures | N/A | Production code에 암호화 없음, build signing은 env var를 올바르게 사용 |
| A03 Injection | N/A | SQL/shell/template injection 없음, VALID_URL regex 분석 결과 ReDoS 없음(quantifier group이 모호하지 않은 character class를 사용) |
| A06 Vulnerable Components | LOW | kuromoji 0.9.0(2013)은 매우 오래되었지만 NLP-only이고 network 없음, lingua 1.2.2, jackson 2.21.3, eclipse-collections 13.0.0은 현재 알려진 CVE 없음 |
| A07 Auth Failures | N/A | 인증 계층 없음 |
| A10 SSRF | N/A | Production source code에서 outbound HTTP 없음 |
