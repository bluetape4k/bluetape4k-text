# README Visual Semantics

## Context

The root README visual overview existed, but the README module table omitted the BOM project and the visual grouping order made the core tokenizer model layer less prominent than secondary modules.

## Decision

Keep the first README visual as an English-only overview, and order module orientation as BOM, core models, language detection, search, Japanese tokenizer, and Korean tokenizer. Normalize public dependency coordinates to `projectGroup=io.github.bluetape4k.text` and current Gradle artifact names.

## Outcome

Updated root README module tables, regenerated root overview and module chart PNGs, fixed localized diagram alt text, and removed stale `io.bluetape4k:*` dependency examples from README files.

## Verification

- `rsvg-convert` regenerated updated PNG assets from SVG sources.
- `xmllint --noout` passed for updated root SVG assets.
- `./gradlew -q projects` confirmed current projects: `:bluetape4k-text-bom`, `:tokenizer-core`, `:lingua`, `:text-search`, `:tokenizer-japanese`, and `:tokenizer-korean`.
- Visual inspection confirmed centered labels and readable layout.

## Future Guidance

For text README updates, treat `settings.gradle.kts` and `gradle.properties` as the source of truth for dependency coordinates, not older branded module names left in module-level examples.
