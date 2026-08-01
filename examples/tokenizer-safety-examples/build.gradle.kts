application {
    mainClass.set("io.bluetape4k.text.examples.tokenizer.TokenizerSafetyExamplesKt")
}

dependencies {
    implementation(project(":lingua"))
    implementation(project(":tokenizer-core"))
    implementation(project(":tokenizer-korean"))
    implementation(project(":tokenizer-japanese"))
    implementation(project(":text-search"))

    testImplementation(bt4k.bluetape4k.junit5)
}
