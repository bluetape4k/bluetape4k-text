# Issue 154 Assertion Style Cleanup

## 배경

Text 저장소 test에는 아직 Java/JUnit style 또는 scalar assertion pattern이 몇 가지 남아
있었다.

- `.size shouldBeEqualTo n`
- `expr shouldBeEqualTo true/false`
- `kotlin.test.assertFailsWith`
- `org.junit.jupiter.api.assertThrows`

이 pattern들은 동작하지만 현재 bluetape4k Kotlin assertion guidance를 따르지 않는다.

## 결정

Test를 bluetape4k assertion intent matcher로 정규화한다.

- `collection shouldHaveSize n`
- `predicate.shouldBeTrue()` / `predicate.shouldBeFalse()`
- `io.bluetape4k.assertions.assertFailsWith<T> { ... }`

Cleanup은 기계적으로 유지하고 production behavior change를 피한다.

## 결과

Cleanup은 `text-search`, `tokenizer-core`, `tokenizer-korean` test와 `TrieCore` KDoc example을
수정했다. 전체 test verification은 통과했다.

## 향후 지침

- Test를 수정할 때는 commit 전에 assertion API drift를 scan한다.
- bluetape4k assertion이 있으면 scalar 값 투영보다 matcher intent를 우선한다.
- 앞으로 forbidden-pattern scan에는 `kotlin.test.assertFailsWith`뿐 아니라 JUnit
  `assertThrows`도 포함한다.
