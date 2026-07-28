# Issue 154 Assertion Style 검토

## 범위

- 이슈: #154 `test: normalize repo-wide assertion style`
- 마일스톤: 0.3.0
- 브랜치: `test/issue-154-assertion-style`
- 검토 대상: `text-search`, `tokenizer-core`, `tokenizer-korean` 전반의 Kotlin test assertion style 정리.

## 7-Tier 검토

| Tier | 결과 | 증거 |
|---|---|---|
| P0 Correctness | PASS | Assertion rewrite는 같은 expected value와 exception type을 보존한다. Production 변경은 `TrieCore.kt`의 KDoc/example text로 제한된다. |
| P1 Runtime Safety | PASS | Runtime code path나 public API 동작은 바뀌지 않았다. CodeGraph affected-flow lookup은 graph index가 `0 nodes`라는 caveat와 함께 변경 파일 14개에 대해 `0 flow(s) affected`를 보고했다. |
| P2 bluetape4k Patterns | PASS | Scalar `.size shouldBeEqualTo` check를 `shouldHaveSize`로, boolean equality를 `.shouldBeTrue()`/`.shouldBeFalse()`로, JUnit/kotlin.test exception check를 bluetape4k `assertFailsWith`로 교체했다. |
| P3 Test Quality | PASS | Assertion DSL 정리 후 전체 test suite가 통과했다. Assertion은 collection, boolean, exception 의도를 직접 표현한다. |
| P4 Scope Control | PASS | 변경은 assertion import/call과 영향받은 source KDoc의 README-facing example comment로 제한된다. 새 dependency나 abstraction은 도입하지 않았다. |
| P5 Build Hygiene | PASS | `git diff --check`, `compileTestKotlin`, 전체 `test`가 성공적으로 끝났다. |
| P6 Process | PASS | Issue #154는 milestone `0.3.0`, label `test`, `refactoring`, `kotlin-quality`, assignee `debop`으로 생성됐다. |

## 검증

- `rg -n "kotlin\\.test\\.|import kotlin\\.test|org\\.junit\\.jupiter\\.api\\.assertThrows|assertThrows\\(|\\.shouldBeEqualTo\\(|shouldBeEqualTo\\s+(true|false)|\\.size\\s+shouldBeEqualTo|\\.size\\.shouldBeEqualTo|assertEquals\\(|assertTrue\\(|assertFalse\\(|assertNull\\(|assertNotNull\\(|assertFailsWith\\(|assertFails\\(|fail\\(" --glob '*.kt' --glob '!build/**' --glob '!.worktrees/**'`
  - PASS: match 없음.
- `git diff --check`
  - PASS.
- `./gradlew compileTestKotlin --no-configuration-cache`
  - PASS: `BUILD SUCCESSFUL in 1s`.
- `./gradlew test --no-configuration-cache`
  - PASS: `BUILD SUCCESSFUL in 826ms`.
- CodeGraph `get_affected_flows_tool`
  - 제한부 PASS: `0 flow(s) affected by changes in 14 file(s)`, 현재 graph index는 `0 nodes`로 보고됨.

## 잔여 위험

- 이 Codex session에서는 IDE diagnostic을 사용할 수 없었다.
- 현재 graph index가 비어 있어 CodeGraph impact analysis의 가치는 낮았다. Gradle compile/test 결과를 primary verification evidence로 삼는다.
