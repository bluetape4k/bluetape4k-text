configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    api(bt4k.bluetape4k.io)
    testImplementation(bt4k.bluetape4k.junit5)

    // Jackson
    testImplementation(bt4k.bluetape4k.jackson3)
    testImplementation(libs.jackson3.module.kotlin)
    testImplementation(libs.jackson3.module.blackbird)

    // Coroutines
    api(bt4k.bluetape4k.coroutines)
    api(libs.kotlinx.coroutines.core)
    testImplementation(libs.kotlinx.coroutines.test)
}
