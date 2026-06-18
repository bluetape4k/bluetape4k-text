# bluetape4k-text-bom

[한국어](./README.ko.md) | English

Maven BOM (Bill of Materials) for the **bluetape4k-text** ecosystem. Import it when an application
uses one or more `io.github.bluetape4k.text:*` runtime artifacts and you want their versions aligned.

The BOM does not provide tokenizer, language-detection, or search APIs by itself. It only publishes
dependency constraints; add the runtime artifacts you actually use as normal dependencies.

## Architecture

![bom Architecture diagram](../docs/images/readme-diagrams/bom-architecture-01.png)

The BOM is a Gradle `java-platform` that publishes `<dependencyManagement>` constraints only. Runtime
classes come from modules such as `tokenizer-korean`, `lingua`, or `text-search`.

## Core Features

- Centralized version management for all published `bluetape4k-text` runtime artifacts
- No runtime classes, auto-configuration, or APIs in the BOM artifact
- Compatible with Gradle `platform(...)`, Spring dependency-management, and Maven import scope
- Aggregated by `bluetape4k-dependencies` for cross-ecosystem version coordination

## Modules Managed

| Artifact | Description |
|----------|-------------|
| `tokenizer-core` | Tokenizer contracts, options, and dictionary utilities |
| `tokenizer-korean` | Korean normalization, tokenization, phrase extraction, stemming, and blockwords |
| `tokenizer-japanese` | Japanese tokenization with Kuromoji IPAdic and blockwords |
| `lingua` | LanguageDetector helpers and Unicode block filters |
| `text-search` | Immutable Aho-Corasick multi-pattern search and replacement |

## Usage Examples

### Gradle Kotlin DSL

```kotlin
plugins {
    id("io.spring.dependency-management") version "1.1.x"
}

dependencyManagement {
    imports {
        mavenBom("io.github.bluetape4k.text:bluetape4k-text-bom:<version>")
    }
}

dependencies {
    implementation("io.github.bluetape4k.text:tokenizer-korean")
    implementation("io.github.bluetape4k.text:text-search")
}
```

### Plain Gradle

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k.text:bluetape4k-text-bom:<version>"))
    implementation("io.github.bluetape4k.text:tokenizer-korean")
}
```

### Maven

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>io.github.bluetape4k.text</groupId>
            <artifactId>bluetape4k-text-bom</artifactId>
            <version>${bluetape4k-text.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

## Configuration Options

The BOM itself has no configuration. For SNAPSHOT builds, add the Sonatype Central Snapshots repository:

```kotlin
repositories {
    mavenCentral()
    maven {
        name = "central-snapshots"
        url = uri("https://central.sonatype.com/repository/maven-snapshots/")
    }
}
```

## Dependency Strategy

This BOM is automatically aggregated by `bluetape4k-dependencies`. Prefer importing
`io.github.bluetape4k:bluetape4k-dependencies` when consuming multiple bluetape4k ecosystems. Import
`bluetape4k-text-bom` directly when the application only needs the text stack.
