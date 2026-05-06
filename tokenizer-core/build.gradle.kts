configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    api(libs.bluetape4k.io)
    testImplementation(libs.bluetape4k.junit5)

    // Jackson
    testImplementation(libs.bluetape4k.jackson2)
    testImplementation(libs.jackson.module.kotlin)
    testImplementation(libs.jackson.module.blackbird)

    // Coroutines
    api(libs.bluetape4k.coroutines)
    api(libs.kotlinx.coroutines.core)
    testImplementation(libs.kotlinx.coroutines.test)
}
