# Security Audit — bluetape4k-text (STRIDE + OWASP Full)

**Date:** 2026-05-17  
**Scope:** All `src/main` sources — tokenizer-core, tokenizer-korean, tokenizer-japanese, lingua, text-search  
**Focus:** OWASP Top 10, deserialization risks, secrets leakage, injection in text processing, unsafe defaults  
**Iterations:** Standard (comprehensive coverage)

---

## Summary

| Severity | Count |
|---|---|
| Critical | 0 |
| High | 0 |
| Medium | 3 |
| Low | 1 |

- **STRIDE Coverage:** T[✓] R[✓] I[✓] D[✓] — S/E N/A (no auth model)
- **OWASP Coverage:** A02[✓] A03[✓] A04[✓] A05[✓] A06[✓] A08[✓] A09[✓] — A01/A07/A10 N/A (library)
- **Confirmed:** 4 | Likely: 0 | Possible: 0

**Context:** bluetape4k-text is a pure JVM NLP library with no HTTP/DB/auth surface. Critical/High OWASP categories (broken access control, auth failures, SSRF) are structurally N/A. Security risk is limited to: PII leakage through exception messages, DoS through unbounded input, and deserialization integrity via missing serialVersionUID.

No hardcoded secrets found. Build signing correctly uses environment variables only.

---

## Top Findings

1. [MEDIUM] [Exception messages embed user text — PII/info disclosure](./findings.md#medium-finding-1-exception-messages-embed-user-input-text--pii-disclosure)
2. [MEDIUM] [No maximum input length — DoS risk for web wrappers](./findings.md#medium-finding-2-no-maximum-input-length-validation--dos-risk-for-web-service-wrappers)
3. [MEDIUM] [Missing serialVersionUID in 20+ Serializable classes](./findings.md#medium-finding-3-missing-serialversionuid-in-20-serializable-classes--deserialization-integrity)
4. [LOW]    [DictionaryProvider exposes classpath path in error message](./findings.md#low-finding-4-dictionaryprovider-path-in-error-message--internal-path-disclosure)

---

## Files in This Report

- [Threat Model](./threat-model.md) — STRIDE analysis, assets, trust boundaries
- [Findings](./findings.md) — all findings ranked by severity
- [Iteration Log](./security-audit-results.tsv) — raw audit log
