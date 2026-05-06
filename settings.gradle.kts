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
