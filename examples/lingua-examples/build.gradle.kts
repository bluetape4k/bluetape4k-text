application {
    mainClass.set("io.bluetape4k.text.examples.lingua.LinguaExamplesKt")
}

dependencies {
    implementation(project(":lingua"))

    testImplementation(bt4k.bluetape4k.junit5)
}
