plugins {
    `java-platform`
    `maven-publish`
    signing
}

dependencies {
    constraints {
        rootProject.subprojects {
            if (name != "bluetape4k-text-bom") {
                api(this)
            }
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("BluetapeText") {
            from(components["javaPlatform"])
            pom {
                name.set("bluetape4k-text-bom")
                description.set("BOM for bluetape4k-text — text processing modules (tokenizer, language detection, search)")
                url.set("https://github.com/bluetape4k/bluetape4k-text")
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                developers {
                    developer {
                        id.set("debop")
                        name.set("Sunghyouk Bae")
                        email.set("sunghyouk.bae@gmail.com")
                    }
                }
                scm {
                    connection.set("scm:git:git://github.com/bluetape4k/bluetape4k-text.git")
                    developerConnection.set("scm:git:ssh://github.com/bluetape4k/bluetape4k-text.git")
                    url.set("https://github.com/bluetape4k/bluetape4k-text")
                }
            }
        }
    }
}

configurePublishingSigning("BluetapeText")
