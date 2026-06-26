application {
    mainClass.set("io.bluetape4k.text.examples.search.TextSearchExamplesKt")
}

dependencies {
    implementation(project(":text-search"))

    testImplementation(libs.bluetape4k.junit5)
    testImplementation(libs.kotlinx.coroutines.test)
}
