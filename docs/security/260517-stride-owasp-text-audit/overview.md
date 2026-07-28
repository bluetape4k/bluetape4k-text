# 보안 감사 — bluetape4k-text (STRIDE + OWASP 전체)

**날짜:** 2026-05-17
**범위:** 전체 `src/main` source — tokenizer-core, tokenizer-korean, tokenizer-japanese, lingua, text-search
**초점:** OWASP Top 10, 역직렬화 위험, secret 유출, 텍스트 처리 injection, 안전하지 않은 기본값
**반복:** Standard(포괄 coverage)

---

## 요약

| 심각도 | 건수 |
|---|---|
| Critical | 0 |
| High | 0 |
| Medium | 3 |
| Low | 1 |

- **STRIDE Coverage:** T[✓] R[✓] I[✓] D[✓] — S/E N/A(auth model 없음)
- **OWASP Coverage:** A02[✓] A03[✓] A04[✓] A05[✓] A06[✓] A08[✓] A09[✓] — A01/A07/A10 N/A(library)
- **확인됨:** 4 | 가능성 높음: 0 | 가능성 있음: 0

**맥락:** bluetape4k-text는 HTTP/DB/auth surface가 없는 순수 JVM NLP library다. Broken access control, auth failures, SSRF 같은 Critical/High OWASP category는 구조적으로 N/A다. 보안 위험은 예외 메시지를 통한 PII 유출, unbounded input에 따른 DoS, serialVersionUID 누락에 따른 역직렬화 무결성 문제로 제한된다.

Hardcoded secret은 발견되지 않았다. Build signing은 환경 변수만 올바르게 사용한다.

---

## 주요 발견 사항

1. [MEDIUM] [예외 메시지가 사용자 텍스트를 포함함 — PII/정보 노출](./findings.md#medium-발견-1-예외-메시지가-사용자-입력-텍스트를-포함함--pii-노출)
2. [MEDIUM] [최대 입력 길이 없음 — web wrapper의 DoS 위험](./findings.md#medium-발견-2-최대-입력-길이-검증-부재--웹-서비스-wrapper의-dos-위험)
3. [MEDIUM] [20개 이상 Serializable class에 serialVersionUID 누락](./findings.md#medium-발견-3-20개-이상-serializable-class에-serialversionuid-누락--역직렬화-무결성)
4. [LOW] [DictionaryProvider가 오류 메시지에서 classpath path를 노출](./findings.md#low-발견-4-dictionaryprovider-오류-메시지의-path--내부-경로-노출)

---

## 이 보고서의 파일

- [위협 모델](./threat-model.md) — STRIDE 분석, asset, trust boundary
- [발견 사항](./findings.md) — 심각도 순으로 정렬한 전체 발견 사항
- [반복 로그](./security-audit-results.tsv) — raw audit log
