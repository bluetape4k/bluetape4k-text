# Dictionary And Blockword Update Pipeline Plan

Date: 2026-05-27
Issue: #85
Milestone: 0.2.0

## Context

Tokenizer and blockword data currently live as classpath resources and can be
augmented at runtime through Korean and Japanese processor/provider APIs. 0.2.0
should define the update path before regular dictionary changes happen, while
runtime reload/versioned dictionary support remains a 0.3.0 feature.

## Current Source Of Truth

| Module | Resource root | Loader |
|---|---|---|
| tokenizer-core | generic classpath paths | `DictionaryProvider` |
| tokenizer-korean | `koreantext` | `KoreanDictionaryProvider` |
| tokenizer-japanese | `japanesetext` | `JapaneseDictionaryProvider` |

## Update Workflow

1. Prepare source data in UTF-8 text files under the owning module resource
   root.
2. Normalize each line by trimming whitespace and removing blank rows.
3. Sort rows deterministically where source order has no semantic meaning.
4. Remove duplicates within the same dictionary file.
5. Run module tests that load the touched resource files.
6. Add release notes that name the changed dictionary area and the validation
   command.

## Validation Matrix

| Change type | Required validation |
|---|---|
| Korean noun/POS dictionary | `./gradlew :tokenizer-korean:test` |
| Korean blockword dictionary | `./gradlew :tokenizer-korean:test --tests "io.bluetape4k.tokenizer.korean.block.KoreanBlockwordProcessorTest"` |
| Japanese blockword dictionary | `./gradlew :tokenizer-japanese:test --tests "io.bluetape4k.tokenizer.japanese.block.JapaneseBlockwordProcessorTest"` |
| Shared dictionary utility behavior | `./gradlew :tokenizer-core:test --tests "io.bluetape4k.tokenizer.utils.DictionaryProviderTest"` |

## Release Notes Template

```markdown
### Dictionary updates

- Area: Korean noun dictionary / Korean blockwords / Japanese blockwords
- Source files: <paths>
- Validation: <commands>
- Compatibility: no public API change / behavior changed for <examples>
```

## Future Work

Issue #102 owns versioned dictionary update and runtime reload support. That work
should add explicit version metadata, reload concurrency rules, and rollback
behavior. The 0.2.0 plan only defines the repeatable manual update path.
