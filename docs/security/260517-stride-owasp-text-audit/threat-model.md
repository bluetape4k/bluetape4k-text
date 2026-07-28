# 위협 모델 — bluetape4k-text

**날짜:** 2026-05-17
**범위:** 전체 `src/main` source — tokenizer-core, tokenizer-korean, tokenizer-japanese, lingua, text-search

---

## Asset 인벤토리

| Asset | 유형 | 우선순위 | 메모 |
|---|---|---|---|
| 사용자 제공 text(tokenize / blockword input) | 사용자 입력 surface | HIGH | PII를 포함할 수 있음 |
| Blockword dictionary(classpath resource) | 정적 data | MEDIUM | Startup 때 로드, classpath 범위 |
| `BlockwordOptions.mask` field | 소비자 config | LOW | End-user 제어 값 아님 |
| Classpath `ClassLoader` reference | 런타임 resource | LOW | 소비자가 override 가능 |
| Maven Central signing key(`SIGNING_KEY`) | Build secret | HIGH | env var에서만 로드, source에는 없음 |
| `CENTRAL_USERNAME` / `CENTRAL_PASSWORD` | Build secret | HIGH | env var에서만 로드, source에는 없음 |

---

## Trust Boundary

```text
Library Consumer (calling code)
  │
  ├── BlockwordRequest / TokenizeRequest (text, options) — TRUST BOUNDARY
  │     └── text: user-generated content; options: consumer-controlled
  │
  ├── DictionaryProvider.readFileByLineFromResources(path, classLoader)
  │     └── path: consumer-controlled; classLoader: consumer-overridable
  │
  └── KoreanProcessor / JapaneseProcessor / AhoCorasickAutomaton
        └── Pure in-process NLP; no network, no filesystem, no DB
```

---

## STRIDE 분석

| Asset | S | T | R | I | D | E | 메모 |
|---|---|---|---|---|---|---|---|
| 사용자 text input | N/A | LOW | N/A | MEDIUM | MEDIUM | N/A | I: 예외 메시지의 PII, D: 입력 크기 제한 없음 |
| Blockword dictionary loading | N/A | N/A | N/A | LOW | N/A | N/A | `classLoader.getResourceAsStream`, classpath 범위 |
| Serializable model class | N/A | MEDIUM | N/A | N/A | N/A | N/A | serialVersionUID 누락 → 역직렬화 무결성 |
| Build secret | N/A | N/A | N/A | LOW | N/A | N/A | Env var only, source에 없음, 올바르게 처리됨 |

**S**(Spoofing) = N/A — auth layer 없음, library only
**E**(Elevation of Privilege) = N/A — privilege model 없음, library only
**A10**(SSRF) = N/A — production code에서 outbound HTTP 없음
**A01**(Broken Access Control) = N/A — access control 없음, library only
**A07**(Auth Failures) = N/A — authentication 없음, library only
