# 한국어 현지화 범위

이 문서는 Korean-localization issue train의 범위 guard를 정의한다. 이후 stacked
PR은 이 문서를 기준으로 primary rewrite scope, 제외 대상, manual parity를 검증한다.

## Primary Scope

- 단일 언어 user-facing Markdown 문서를 한국어로 재작성한다.
- Kotlin/KTS 주석과 KDoc을 한국어로 재작성한다.
- 기존 `@property`, `@param`, `@return` 및 관련 API 설명이 있는 곳은 상세한
  한국어 설명으로 보강한다.

## Primary Rewrite Scope 제외 대상

- `README.md` 및 `README.ko.md` 파일.
- `AGENTS.md`, `CLAUDE.md`, `SKILL.md`, prompts 및 기타 LLM-facing operating
  guidance.
- `docs/manual/en/**` 및 `docs/manual/ko/**` bilingual manual pair. 이 경로는
  parity만 검증한다.
- Production behavior, public API name, serialized name, build coordinate,
  dependency version, release metadata.

## 현재 Inventory

Issue #174 기준 baseline inventory는 다음과 같다.

| Surface | Count |
|---|---:|
| Candidate single-language Markdown | 69 |
| Excluded README files | 17 |
| Excluded LLM-facing documents | 2 |
| Bilingual manual files | 48 |
| Manual parity | EN=24 / KO=24 |
| Kotlin/KTS files | 148 |
| Kotlin/KTS files with comment-like lines | 109 |
| Comment-like lines | 5,964 |
| KDoc tags | 164 |

Primary Markdown group:

| Group | Files |
|---|---:|
| `CHANGELOG.md` | 1 |
| `WIP.md` | 1 |
| `docs/governance` | 1 |
| `docs/lessons` | 50 |
| `docs/localization` | 1 |
| `docs/review` | 5 |
| `docs/security` | 3 |
| `docs/superpowers` | 7 |

Kotlin/KTS comment group:

| Group | Comment-like Lines |
|---|---:|
| `build.gradle.kts` | 5 |
| `buildSrc` | 20 |
| `lingua` | 194 |
| `text-search` | 1,340 |
| `tokenizer-core` | 1,556 |
| `tokenizer-japanese` | 384 |
| `tokenizer-korean` | 2,465 |

## 검증

실행:

```bash
ruby scripts/localization/audit_scope.rb
```

이 명령은 manual EN/KO path가 갈라지거나 제외된 Markdown surface가 primary rewrite
set에 들어오면 실패한다. 명령 출력은 PR DoD evidence로 사용할 수 있도록 현재
inventory를 JSON으로 출력한다.
