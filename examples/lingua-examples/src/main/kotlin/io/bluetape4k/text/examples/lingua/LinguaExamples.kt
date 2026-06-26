package io.bluetape4k.text.examples.lingua

import com.github.pemistahl.lingua.api.Language
import io.bluetape4k.lingua.detectAllLanguagesOf
import io.bluetape4k.lingua.languageDetectorOf
import java.io.Serial
import java.io.Serializable

internal data class LinguaExampleReport(
    val reusedDetectorLanguages: Set<Language>,
    val subsetLanguages: Set<Language>,
    val lowAccuracyLanguage: Language,
): Serializable {
    companion object {
        @Serial
        private const val serialVersionUID: Long = 1L
    }
}

internal fun runLinguaExamples(): LinguaExampleReport {
    val languages = setOf(Language.ENGLISH, Language.KOREAN, Language.JAPANESE)
    val mixedText = "Hello service. 안녕하세요. こんにちは。"
    val reusedDetector = languageDetectorOf(
        languages = languages,
        minimumRelativeDistance = 0.0,
        isEveryLanguageModelPreloaded = true,
        isLowAccuracyModeEnabled = false,
    )
    val subsetDetector = languageDetectorOf(languages) {
        withMinimumRelativeDistance(0.0)
        withPreloadedLanguageModels()
    }
    val lowAccuracyDetector = languageDetectorOf(
        languages = setOf(Language.ENGLISH, Language.GERMAN),
        minimumRelativeDistance = 0.0,
        isEveryLanguageModelPreloaded = false,
        isLowAccuracyModeEnabled = true,
    )

    return LinguaExampleReport(
        reusedDetectorLanguages = reusedDetector.detectAllLanguagesOf(mixedText),
        subsetLanguages = subsetDetector.detectAllLanguagesOf(mixedText),
        lowAccuracyLanguage = lowAccuracyDetector.detectLanguageOf("Hello service users"),
    )
}

internal fun renderLinguaExampleReport(report: LinguaExampleReport): String =
    buildString {
        appendLine("reuse=${report.reusedDetectorLanguages.joinToString()}")
        appendLine("subset=${report.subsetLanguages.joinToString()}")
        appendLine("lowAccuracy=${report.lowAccuracyLanguage}")
    }

fun main() {
    println(renderLinguaExampleReport(runLinguaExamples()))
}
