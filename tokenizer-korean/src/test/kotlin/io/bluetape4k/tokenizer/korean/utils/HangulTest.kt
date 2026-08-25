package io.bluetape4k.tokenizer.korean.utils

import io.bluetape4k.logging.KLogging
import io.bluetape4k.tokenizer.korean.TestBase
import io.bluetape4k.tokenizer.korean.utils.Hangul.HangulChar
import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeTrue
import org.junit.jupiter.api.Test


class HangulTest: TestBase() {

    companion object: KLogging()

    @Test
    fun `decompose KoreanChar with coda`() {
        Hangul.decomposeHangul('간') shouldBeEqualTo HangulChar('ㄱ', 'ㅏ', 'ㄴ')
        Hangul.decomposeHangul('관') shouldBeEqualTo HangulChar('ㄱ', 'ㅘ', 'ㄴ')
        Hangul.decomposeHangul('꼃') shouldBeEqualTo HangulChar('ㄲ', 'ㅕ', 'ㅀ')
    }

    @Test
    fun `decompose KoreanChar without coda`() {
        Hangul.decomposeHangul('가') shouldBeEqualTo HangulChar('ㄱ', 'ㅏ', ' ')
        Hangul.decomposeHangul('과') shouldBeEqualTo HangulChar('ㄱ', 'ㅘ', ' ')
        Hangul.decomposeHangul('껴') shouldBeEqualTo HangulChar('ㄲ', 'ㅕ', ' ')
    }

    @Test
    fun `decomposeHangul with invalid char`() {
        assertFailsWith<IllegalArgumentException> {
            Hangul.decomposeHangul('ㅋ')
        }

        assertFailsWith<IllegalArgumentException> {
            Hangul.decomposeHangul('ㅏ')
        }

        assertFailsWith<IllegalArgumentException> {
            Hangul.decomposeHangul('ㅀ')
        }

        assertFailsWith<IllegalArgumentException> {
            Hangul.decomposeHangul('A')
        }

        assertFailsWith<IllegalArgumentException> {
            Hangul.decomposeHangul('\uD7A4')
        }
    }

    @Test
    fun `hasCoda with Coda`() {
        Hangul.hasCoda('갈').shouldBeTrue()
        Hangul.hasCoda('갉').shouldBeTrue()
    }

    @Test
    fun `hasCoda without Coda`() {
        Hangul.hasCoda('가').shouldBeFalse()
        Hangul.hasCoda('ㄱ').shouldBeFalse()
        Hangul.hasCoda('ㅘ').shouldBeFalse()
        Hangul.hasCoda('ㅀ').shouldBeFalse()
        Hangul.hasCoda(' ').shouldBeFalse()
        Hangul.hasCoda('A').shouldBeFalse()
        Hangul.hasCoda('\uD7A4').shouldBeFalse()
    }

    @Test
    fun `compose KoreanChar with coda`() {
        Hangul.composeHangul('ㄱ', 'ㅏ', 'ㄷ') shouldBeEqualTo '갇'
        Hangul.composeHangul('ㄲ', 'ㅑ', 'ㅀ') shouldBeEqualTo '꺓'
        Hangul.composeHangul('ㅊ', 'ㅘ', 'ㄴ') shouldBeEqualTo '촨'
    }

    @Test
    fun `compose KoreanChar without coda`() {
        Hangul.composeHangul('ㄱ', 'ㅏ', ' ') shouldBeEqualTo '가'
        Hangul.composeHangul('ㄲ', 'ㅑ', ' ') shouldBeEqualTo '꺄'
        Hangul.composeHangul('ㅊ', 'ㅘ', ' ') shouldBeEqualTo '촤'
    }

    @Test
    fun `compose only vowel`() {
        assertFailsWith<IllegalArgumentException> {
            Hangul.composeHangul(' ', 'ㅏ', ' ')
        }

        assertFailsWith<IllegalArgumentException> {
            Hangul.composeHangul('ㄲ', ' ', ' ')
        }

        assertFailsWith<IllegalArgumentException> {
            Hangul.composeHangul(' ', ' ', 'ㄴ')
        }

        assertFailsWith<IllegalArgumentException> {
            Hangul.composeHangul('A', 'ㅏ')
        }

        assertFailsWith<IllegalArgumentException> {
            Hangul.composeHangul('ㄱ', 'A')
        }

        assertFailsWith<IllegalArgumentException> {
            Hangul.composeHangul('ㄱ', 'ㅏ', 'A')
        }
    }
}
