application {
    mainClass.set("io.bluetape4k.text.examples.lingua.LinguaExamplesKt")
}

dependencies {
    implementation(project(":lingua"))

    testImplementation(libs.bluetape4k.junit5)
}
