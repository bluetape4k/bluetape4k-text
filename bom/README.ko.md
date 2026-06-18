# bluetape4k-text-bom

한국어 | [English](./README.md)

**bluetape4k-text** 생태계용 Maven BOM (Bill of Materials). 애플리케이션이 하나 이상의
`io.github.bluetape4k.text:*` runtime artifact를 사용할 때 버전을 맞추기 위해 import한다.

BOM 자체는 tokenizer, language detection, search API를 제공하지 않는다. dependency constraint만
게시하므로 실제로 사용할 runtime artifact는 별도 dependency로 추가해야 한다.

## 아키텍처

![bom Architecture diagram](../docs/images/readme-diagrams/bom-architecture-01.png)

BOM은 Gradle `java-platform` 으로 `<dependencyManagement>` constraint만 게시한다. Runtime class는
`tokenizer-korean`, `lingua`, `text-search` 같은 모듈에서 제공한다.

## 핵심 기능

- 게시되는 모든 `bluetape4k-text` runtime artifact 버전 중앙 관리
- BOM artifact에는 runtime class, auto-configuration, API가 없음
- Gradle `platform(...)`, Spring dependency-management, Maven import scope와 함께 사용 가능
- `bluetape4k-dependencies` 가 상위에서 통합

## 관리 모듈

| Artifact | 설명 |
|----------|------|
| `tokenizer-core` | Tokenizer contract, option, dictionary utility |
| `tokenizer-korean` | 한국어 정규화, 토큰화, 구문 추출, stemming, blockword |
| `tokenizer-japanese` | Kuromoji IPAdic 기반 일본어 토큰화와 blockword |
| `lingua` | LanguageDetector helper와 Unicode block filter |
| `text-search` | Immutable Aho-Corasick 다중 패턴 검색과 replacement |

## 사용 예제

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

### 순수 Gradle

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

## 설정 옵션

BOM 자체는 별도 설정이 없다. SNAPSHOT 사용 시 Sonatype Central Snapshots 저장소 추가:

```kotlin
repositories {
    mavenCentral()
    maven {
        name = "central-snapshots"
        url = uri("https://central.sonatype.com/repository/maven-snapshots/")
    }
}
```

## 의존성 전략

이 BOM은 `bluetape4k-dependencies` 에서 자동 통합된다. 여러 bluetape4k 생태계를 함께 사용한다면
`io.github.bluetape4k:bluetape4k-dependencies` import를 권장한다. 애플리케이션이 text stack만
사용한다면 `bluetape4k-text-bom`을 직접 import하면 된다.
