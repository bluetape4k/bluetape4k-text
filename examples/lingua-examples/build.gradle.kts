application {
    mainClass.set("io.bluetape4k.text.examples.lingua.LinguaExamplesKt")
}

dependencies {
    implementation(project(":lingua"))
    implementation(project(":tokenizer-korean"))
    implementation(project(":tokenizer-japanese"))

    testImplementation(bt4k.bluetape4k.junit5)
}
