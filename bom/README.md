# bluetape4k-text-bom

[한국어](./README.ko.md) | English

Maven BOM (Bill of Materials) for the **bluetape4k-text** ecosystem. Manages versions of all
`io.github.bluetape4k.text:*` modules so consumers can declare dependencies without specifying
individual versions.

## Architecture

![bom Architecture diagram](../docs/images/readme-diagrams/bom-architecture-01.png)

The BOM is a Gradle `java-platform` that publishes only `<dependencyManagement>` constraints — no runtime classes.

## Core Features

- Centralized version management for all `bluetape4k-text` modules
- Single source of truth for tokenizer + search + language detection stack
- Aggregated by `bluetape4k-dependencies` for cross-ecosystem version coordination

## Modules Managed

| Module | Description |
|--------|-------------|
| `bluetape4k-tokenizer-core` | Tokenizer core abstractions |
| `bluetape4k-tokenizer-korean` | Korean morphological tokenizer |
| `bluetape4k-tokenizer-japanese` | Japanese tokenizer |
| `bluetape4k-text-search` | Aho-Corasick multi-pattern search |
| `bluetape4k-lingua` | Multi-language detection |

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
    implementation("io.github.bluetape4k.text:bluetape4k-tokenizer-korean")
    implementation("io.github.bluetape4k.text:bluetape4k-text-search")
}
```

### Plain Gradle

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k.text:bluetape4k-text-bom:<version>"))
    implementation("io.github.bluetape4k.text:bluetape4k-tokenizer-korean")
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

## Dependency

This BOM is automatically aggregated by `bluetape4k-dependencies`. Prefer importing
`io.github.bluetape4k:bluetape4k-dependencies` when consuming multiple bluetape4k ecosystems.
