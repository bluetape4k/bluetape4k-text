# 한국어 현지화 최종 검증

이 문서는 #173 한국어 현지화 train의 최종 검증 기록이다. README, LLM-facing 운영 문서, bilingual manual pair는 primary rewrite scope에서 제외하고, manual pair는 parity 대상으로만 확인했다.

## 범위 판정

| 항목 | 결과 |
|---|---|
| Candidate single-language Markdown | 69 |
| Excluded README files | 17 |
| Excluded LLM-facing documents | 2 |
| Bilingual manual files | 48 |
| Manual parity | EN=24 / KO=24 |
| Kotlin/KTS files | 148 |
| Kotlin/KTS files with comment-like lines | 109 |
| Comment-like lines | 6,562 |
| KDoc tags | 579 |

## 제외 범위 확인

다음 경로는 train 전체 diff에서 primary rewrite 변경이 없음을 확인했다.

- `README.md`, `README.ko.md`, `**/README*`
- `AGENTS.md`, `CLAUDE.md`, `SKILL.md`
- `docs/manual/en/**`
- `docs/manual/ko/**`

## Stacked PR Train

| Issue | PR | Scope | 상태 |
|---:|---:|---|---|
| #174 | #193 | localization scope guard | merged |
| #175 | #194 | root single-language Markdown | open |
| #176 | #196 | docs/superpowers | open |
| #177 | #197 | docs/security | open |
| #178 | #198 | docs/governance, docs/review | open |
| #179 | #199 | early docs/lessons | open |
| #180 | #200 | early docs/lessons continuation | open |
| #181 | #201 | diagram docs/lessons | open |
| #182 | #202 | release/catalog docs/lessons | open |
| #183 | #203 | nightly/cache docs/lessons | open |
| #184 | #204 | readiness/quality docs/lessons | open |
| #185 | #205 | tokenizer-core KDoc/comments | open |
| #186 | #206 | text-search KDoc/comments | open |
| #187 | #207 | tokenizer-korean facade/block/normalizer KDoc/comments | open |
| #188 | #208 | tokenizer-korean tokenizer/phrase KDoc/comments | open |
| #189 | #209 | tokenizer-korean utils/stemmer KDoc/comments | open |
| #190 | #210 | tokenizer-japanese KDoc/comments | open |
| #191 | #211 | lingua/buildSrc/Gradle comments | open |
| #192 | 현재 PR | final scope/parity/behavior verification | open |

## 검증 명령

```bash
ruby scripts/localization/audit_scope.rb
ruby scripts/localization/localization_scope_audit_test.rb
git diff --check
./gradlew build --no-daemon
```

검증 결과:

- `ruby scripts/localization/audit_scope.rb`: manual parity `EN=24 / KO=24`, missing pair 없음.
- `ruby scripts/localization/localization_scope_audit_test.rb`: 2 runs, 12 assertions, 0 failures.
- `git diff --check`: 통과.
- `./gradlew build --no-daemon`: `BUILD SUCCESSFUL in 36s`, 99 actionable tasks.

## 잔여 리스크

- PR merge는 별도 승인 게이트다. 각 PR은 exact head, checks, reviews, unresolved thread를 다시 확인한 뒤 merge해야 한다.
- GitHub checks가 보고되지 않는 PR들이 있으므로, merge 직전에도 local build evidence와 PR state를 함께 확인한다.
