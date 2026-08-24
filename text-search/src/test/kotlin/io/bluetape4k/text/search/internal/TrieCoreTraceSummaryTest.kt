package io.bluetape4k.text.search.internal

import io.bluetape4k.assertions.shouldContain
import io.bluetape4k.assertions.shouldNotContain
import org.junit.jupiter.api.Test

class TrieCoreTraceSummaryTest {

    @Test
    fun `safe trace summary keeps metadata without exposing source text`() {
        val source = "password reset secret-123"

        val summary = source.safeTraceSummary()

        summary shouldContain "length=${source.length}"
        summary shouldContain "sha256="
        summary shouldContain "classes=letters:"
        summary shouldNotContain source
        summary shouldNotContain "password"
        summary shouldNotContain "secret-123"
    }
}
