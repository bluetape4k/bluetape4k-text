import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.testing.Test
import java.io.File
import java.net.URI
import java.security.MessageDigest
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.zip.ZipFile

application {
    mainClass.set("io.bluetape4k.text.examples.tokenizer.TokenizerSafetyExamplesKt")
}

private val SUDACHI_DICTIONARY_VERSION = "20260428"
private val SUDACHI_DICTIONARY_ARCHIVE_NAME = "sudachi-dictionary-20260428-core.zip"
private val SUDACHI_DICTIONARY_ENTRY = "sudachi-dictionary-20260428/system_core.dic"
private val SUDACHI_DICTIONARY_ARCHIVE_SHA256 =
    "40c8ffc095283f07aa06cae922e7b8147bf2919ec8830567b0b3f7a7efa3239f"
private val SUDACHI_DICTIONARY_ARCHIVE_SIZE = 72_238_136L
private val SUDACHI_DICTIONARY_SIZE = 217_374_303L
private val SUDACHI_DICTIONARY_URL =
    "https://github.com/WorksApplications/SudachiDict/releases/download/v20260428/sudachi-dictionary-20260428-core.zip"

fun File.sha256(): String {
    val digest = MessageDigest.getInstance("SHA-256")
    inputStream().buffered().use { input ->
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            digest.update(buffer, 0, count)
        }
    }
    return digest.digest().joinToString("") { byte ->
        (byte.toInt() and 0xff).toString(16).padStart(2, '0')
    }
}

val sudachiDictionaryDirectory = layout.buildDirectory.dir("sudachi-dictionary/v$SUDACHI_DICTIONARY_VERSION")
val sudachiDictionaryArchive = sudachiDictionaryDirectory.map { it.file(SUDACHI_DICTIONARY_ARCHIVE_NAME) }
val sudachiSystemDictionary = sudachiDictionaryDirectory.map { it.file("system_core.dic") }

val prepareSudachiDictionary = tasks.register("prepareSudachiDictionary") {
    description = "Downloads and verifies the pinned SudachiDict core dictionary into the build cache."
    group = "documentation"

    inputs.property("sudachiDictionaryUrl", SUDACHI_DICTIONARY_URL)
    inputs.property("sudachiDictionaryArchiveSha256", SUDACHI_DICTIONARY_ARCHIVE_SHA256)
    inputs.property("sudachiDictionaryArchiveSize", SUDACHI_DICTIONARY_ARCHIVE_SIZE)
    outputs.file(sudachiSystemDictionary)
    outputs.upToDateWhen {
        val archive = sudachiDictionaryArchive.get().asFile
        val dictionary = sudachiSystemDictionary.get().asFile
        archive.isFile &&
            archive.length() == SUDACHI_DICTIONARY_ARCHIVE_SIZE &&
            archive.sha256() == SUDACHI_DICTIONARY_ARCHIVE_SHA256 &&
            dictionary.isFile &&
            dictionary.length() == SUDACHI_DICTIONARY_SIZE
    }

    doLast {
        val directory = sudachiDictionaryDirectory.get().asFile
        require(directory.mkdirs() || directory.isDirectory) {
            "Sudachi dictionary directory cannot be created: $directory"
        }

        val archive = sudachiDictionaryArchive.get().asFile
        if (!archive.isFile || archive.length() != SUDACHI_DICTIONARY_ARCHIVE_SIZE || archive.sha256() != SUDACHI_DICTIONARY_ARCHIVE_SHA256) {
            val partial = archive.resolveSibling("${archive.name}.part")
            URI(SUDACHI_DICTIONARY_URL).toURL().openConnection().apply {
                connectTimeout = 30_000
                readTimeout = 120_000
            }.getInputStream().buffered().use { input ->
                partial.outputStream().buffered().use { output -> input.copyTo(output) }
            }
            require(partial.length() == SUDACHI_DICTIONARY_ARCHIVE_SIZE) {
                "Unexpected SudachiDict archive size: ${partial.length()}"
            }
            require(partial.sha256() == SUDACHI_DICTIONARY_ARCHIVE_SHA256) {
                "SudachiDict archive SHA-256 mismatch: $partial"
            }
            Files.move(
                partial.toPath(),
                archive.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
            )
        }

        val dictionary = sudachiSystemDictionary.get().asFile
        ZipFile(archive).use { zip ->
            require(zip.getEntry("sudachi-dictionary-20260428/LEGAL") != null) {
                "SudachiDict archive is missing LEGAL: $archive"
            }
            require(zip.getEntry("sudachi-dictionary-20260428/LICENSE-2.0.txt") != null) {
                "SudachiDict archive is missing LICENSE-2.0.txt: $archive"
            }
            val entry = requireNotNull(zip.getEntry(SUDACHI_DICTIONARY_ENTRY)) {
                "SudachiDict archive is missing $SUDACHI_DICTIONARY_ENTRY"
            }
            require(entry.size == SUDACHI_DICTIONARY_SIZE) {
                "Unexpected SudachiDict entry size: ${entry.size}"
            }
            zip.getInputStream(entry).buffered().use { input ->
                dictionary.outputStream().buffered().use { output -> input.copyTo(output) }
            }
        }
        require(dictionary.length() == SUDACHI_DICTIONARY_SIZE) {
            "Unexpected extracted SudachiDict size: ${dictionary.length()}"
        }
    }
}

dependencies {
    implementation(project(":lingua"))
    implementation(project(":tokenizer-core"))
    implementation(project(":tokenizer-korean"))
    implementation(project(":tokenizer-japanese"))
    implementation(project(":text-search"))
    implementation(bt4k.sudachi)

    testImplementation(bt4k.bluetape4k.junit5)
}

tasks.withType<Test>().configureEach {
    dependsOn(prepareSudachiDictionary)
    systemProperty("bluetape4k.sudachi.system-dictionary", sudachiSystemDictionary.get().asFile.absolutePath)
}

tasks.withType<JavaExec>().configureEach {
    dependsOn(prepareSudachiDictionary)
    systemProperty("bluetape4k.sudachi.system-dictionary", sudachiSystemDictionary.get().asFile.absolutePath)
}
