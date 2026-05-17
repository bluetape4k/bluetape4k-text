# Threat Model — bluetape4k-text

**Date:** 2026-05-17
**Scope:** All `src/main` sources — tokenizer-core, tokenizer-korean, tokenizer-japanese, lingua, text-search

---

## Asset Inventory

| Asset | Type | Priority | Notes |
|---|---|---|---|
| User-supplied text (tokenize / blockword input) | User input surface | HIGH | Potentially contains PII |
| Blockword dictionaries (classpath resources) | Static data | MEDIUM | Loaded at startup; classpath-scoped |
| `BlockwordOptions.mask` field | Consumer config | LOW | Not end-user controlled |
| Classpath `ClassLoader` reference | Runtime resource | LOW | Consumer can override |
| Maven Central signing key (`SIGNING_KEY`) | Build secret | HIGH | Loaded from env var only — not in source |
| `CENTRAL_USERNAME` / `CENTRAL_PASSWORD` | Build secret | HIGH | Loaded from env var only — not in source |

---

## Trust Boundaries

```
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

## STRIDE Analysis

| Asset | S | T | R | I | D | E | Notes |
|---|---|---|---|---|---|---|---|
| User text input | N/A | LOW | N/A | MEDIUM | MEDIUM | N/A | I: PII in exception messages; D: no input size limit |
| Blockword dictionary loading | N/A | N/A | N/A | LOW | N/A | N/A | classLoader.getResourceAsStream — classpath-scoped |
| Serializable model classes | N/A | MEDIUM | N/A | N/A | N/A | N/A | Missing serialVersionUID → deserialization integrity |
| Build secrets | N/A | N/A | N/A | LOW | N/A | N/A | Env var only; not in source — correctly handled |

**S** (Spoofing) = N/A — no auth layer; library only
**E** (Elevation of Privilege) = N/A — no privilege model; library only
**A10** (SSRF) = N/A — zero outbound HTTP in production code
**A01** (Broken Access Control) = N/A — no access control; library only
**A07** (Auth Failures) = N/A — no authentication; library only
