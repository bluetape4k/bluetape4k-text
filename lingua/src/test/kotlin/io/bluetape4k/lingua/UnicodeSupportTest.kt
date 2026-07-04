package io.bluetape4k.lingua

import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class UnicodeSupportTest: AbstractLinguaTest() {

    @Nested
    inner class Ascii {
        @Test
        fun asciiRangeOnlyReturnsTrue() {
            listOf('\u0000', 'A', '9', '\u007F').forEach { it.isAscii.shouldBeTrue() }
            listOf('\u0080', '가', 'あ').forEach { it.isAscii.shouldBeFalse() }
        }
    }

    @Nested
    inner class Latin {
        @Test
        fun latinBlocksReturnTrue() {
            listOf('A', '\u00C7', '\u0100', '\u0180', '\u0250', '\u02B0', '\u1E00', '\u2C60', '\uA720')
                .forEach { it.isLatin.shouldBeTrue() }
        }

        @Test
        fun nonLatinBlocksReturnFalse() {
            listOf('가', 'あ', '中', 'م', 'ก').forEach { it.isLatin.shouldBeFalse() }
        }
    }

    @Nested
    inner class Arabic {
        @Test
        fun arabicBlocksReturnTrue() {
            listOf('\u0627', '\u0750', '\uFB50', '\uFE70').forEach { it.isArabic.shouldBeTrue() }
        }

        @Test
        fun nonArabicBlocksReturnFalse() {
            listOf('A', '가', 'あ', '中', 'ก').forEach { it.isArabic.shouldBeFalse() }
        }
    }

    @Nested
    inner class Thai {
        @Test
        fun thaiBlocksReturnTrue() {
            listOf('\u0E01', '\u1950', '\u1980', '\u1A20').forEach { it.isThai.shouldBeTrue() }
        }

        @Test
        fun nonThaiBlocksReturnFalse() {
            listOf('A', '가', 'あ', '中', 'م').forEach { it.isThai.shouldBeFalse() }
        }
    }

    @Nested
    inner class Korean {
        @Test
        fun hangulBlocksReturnTrue() {
            listOf('\u1100', '\u3131', '\uA960', '가', '\uD7B0', '\uFFA0').forEach { it.isKorean.shouldBeTrue() }
        }

        @Test
        fun nonHangulBlocksReturnFalse() {
            listOf('A', 'あ', '中', 'م', 'ก').forEach { it.isKorean.shouldBeFalse() }
        }
    }

    @Nested
    inner class Japanese {
        @Test
        fun japaneseBlocksReturnTrue() {
            listOf('あ', 'カ', '\u31F0', '\uFF66', '\u2E80', '\u2F00').forEach { it.isJapanese.shouldBeTrue() }
        }

        @Test
        fun nonJapaneseBlocksReturnFalse() {
            listOf('A', '가', '中', 'م', 'ก').forEach { it.isJapanese.shouldBeFalse() }
        }
    }

    @Nested
    inner class Chinese {
        @Test
        fun chineseBmpBlocksReturnTrue() {
            listOf('中', '\u3400').forEach { it.isChinese.shouldBeTrue() }
        }

        @Test
        fun nonChineseBlocksReturnFalse() {
            listOf('A', '가', 'あ', 'م', 'ก').forEach { it.isChinese.shouldBeFalse() }
        }
    }
}
