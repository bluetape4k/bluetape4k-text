pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
    }
    plugins {
        id("org.gradle.toolchains.foojay-resolver-convention") version ("1.0.0")
    }
}

rootProject.name = "bluetape4k-text"

include(
    "tokenizer-core",
    "tokenizer-japanese",
    "tokenizer-korean",
    "lingua",
    "text-search",
)

include("bluetape4k-text-bom")
project(":bluetape4k-text-bom").projectDir = file("bom")
