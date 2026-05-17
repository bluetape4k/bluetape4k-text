# Security Findings — bluetape4k-text

**Date:** 2026-05-17  
**Total:** 0 Critical, 0 High, 3 Medium, 1 Low

---

## [MEDIUM] Finding 1: Exception Messages Embed User Input Text — PII Disclosure

- **OWASP:** A09 — Security Logging and Monitoring Failures
- **STRIDE:** Information Disclosure
- **Confidence:** Confirmed
- **Locations:**
  - `tokenizer-korean/src/main/.../block/KoreanBlockwordProcessor.kt:84`
  - `tokenizer-korean/src/main/.../block/KoreanBlockwordProcessor.kt:137`
  - `tokenizer-japanese/src/main/.../block/JapaneseBlockwordProcessor.kt:142`

### Code Evidence

```kotlin
// KoreanBlockwordProcessor.kt:84
throw TokenizerException("Fail to mask block word. text=[$text]", e)

// KoreanBlockwordProcessor.kt:137
throw TokenizerException("Fail to mask block word. request=$request", e)

// JapaneseBlockwordProcessor.kt:142
throw TokenizerException("Fail to mask block word. request=$request", e)
```

`BlockwordRequest.toString()` includes the full `text` field. When the library is used inside a web service:
1. The full user input appears in exception messages → propagates to server logs
2. If the exception is caught and re-thrown to an HTTP response, user input leaks in the error body
3. If the text contains PII (personal names, medical records, etc.), logs become a PII store

### Attack Scenario

1. Web service receives POST /check-text with body `{"text": "사용자_실명_주민번호_X"}`
2. An internal tokenizer error occurs
3. `TokenizerException("Fail to mask block word. text=[사용자_실명_주민번호_X]")` is thrown
4. Exception propagates to HTTP 500 response or log aggregator
5. PII now visible in logs and potentially in error responses

### Mitigation

```kotlin
// Before
throw TokenizerException("Fail to mask block word. text=[$text]", e)

// After — omit text content; include only structural metadata
throw TokenizerException(
    "Fail to mask block word. textLength=${text.length}, locale=${request.options.locale}",
    e
)
```

---

## [MEDIUM] Finding 2: No Maximum Input Length Validation — DoS Risk for Web Service Wrappers

- **OWASP:** A04 — Insecure Design
- **STRIDE:** Denial of Service
- **Confidence:** Confirmed
- **Locations:**
  - `tokenizer-korean/.../KoreanProcessor.kt` (all public functions)
  - `tokenizer-japanese/.../JapaneseProcessor.kt` (all public functions)
  - `tokenizer-core/.../model/TokenizeRequest.kt`

### Code Evidence

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

Combined with the O(n²) complexity bug in `KoreanChunker` (#42), a 100,000-character input can consume gigabytes of memory and seconds of CPU per request. There is no maximum input length enforced anywhere in the public API.

### Impact

The library itself is not a web server, so this is a **library-level risk** that becomes a real DoS vulnerability when the library is wrapped in a web service without its own input validation. Maven Central consumers building REST APIs on top of this library inherit this risk.

### Mitigation

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

Alternatively, document the lack of input length validation in KDoc so consumers know they must validate themselves.

---

## [MEDIUM] Finding 3: Missing serialVersionUID in 20+ Serializable Classes — Deserialization Integrity

- **OWASP:** A08 — Software and Data Integrity Failures
- **STRIDE:** Tampering (integrity)
- **Confidence:** Confirmed
- **Note:** Already tracked as issue #27

### Classes Missing serialVersionUID

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

Without explicit `serialVersionUID`, the JVM auto-generates one from the class structure. Any field addition/removal/rename causes a UID mismatch → `InvalidClassException` when deserializing previously stored instances (caches, distributed sessions, message queues).

### Mitigation

Add to every `Serializable` class:
```kotlin
companion object {
    private const val serialVersionUID = 1L
}
```

---

## [LOW] Finding 4: DictionaryProvider Path in Error Message — Internal Path Disclosure

- **OWASP:** A05 — Security Misconfiguration
- **STRIDE:** Information Disclosure
- **Confidence:** Confirmed
- **Location:** `tokenizer-core/src/main/.../utils/DictionaryProvider.kt:70`

### Code Evidence

```kotlin
val stream: InputStream? = classLoader.getResourceAsStream(path)
check(stream != null) { "Can't open file. path=$path" }
```

If `path` is consumer-controlled and the resource does not exist, the error message exposes the full classpath resource path. This is low risk for a library (no web-facing API), but the path could reveal internal project structure if the exception propagates to a client.

### Mitigation

```kotlin
check(stream != null) { "Dictionary resource not found: $path" }
// or omit path if it may contain sensitive directory structure
```

---

## N/A Items

| OWASP Category | Verdict | Reason |
|---|---|---|
| A01 Broken Access Control | N/A | No access control; pure library |
| A02 Cryptographic Failures | N/A | No cryptography in production code; build signing uses env vars correctly |
| A03 Injection | N/A | No SQL/shell/template injection; VALID_URL regex analyzed — no ReDoS (quantifier groups have unambiguous character classes) |
| A06 Vulnerable Components | LOW | kuromoji 0.9.0 (2013) is very old but NLP-only, no network; lingua 1.2.2, jackson 2.21.3, eclipse-collections 13.0.0 are current with no known CVEs |
| A07 Auth Failures | N/A | No authentication layer |
| A10 SSRF | N/A | Zero outbound HTTP in production source code |
