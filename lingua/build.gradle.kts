configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    api(bt4k.bluetape4k.core)
    api(libs.lingua)
    testImplementation(bt4k.bluetape4k.junit5)
}
