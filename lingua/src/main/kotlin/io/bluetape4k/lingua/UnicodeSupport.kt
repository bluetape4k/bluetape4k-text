package io.bluetape4k.lingua

/**
 * Returns `true` if this character is an ASCII character (code point `0..127`).
 */
val Char.isAscii: Boolean get() = this.code in 0..127

/**
 * Returns `true` if this character falls within a Latin Unicode block range.
 */
val Char.isLatin: Boolean
    get() = this.code in 0x0000..0x007F ||      // 라틴 문자
            this.code in 0x0080..0x00FF ||      // 라틴 확장
            this.code in 0x0100..0x017F ||      // 라틴 확장 C
            this.code in 0x0180..0x024F ||      // 라틴 확장 D
            this.code in 0x0250..0x02AF ||      // IPA 확장
            this.code in 0x02B0..0x02FF ||      // BIPA 확장
            this.code in 0x1E00..0x1EFF ||      // 라틴 확장 추가
            this.code in 0x2C60..0x2C7F ||      // 라틴 확장 추가
            this.code in 0xA720..0xA7FF         // 라틴 확장 추가 D

/**
 * Returns `true` if this character falls within an Arabic Unicode block range.
 */
val Char.isArabic: Boolean
    get() = this.code in 0x0600..0x06FF ||      // 아랍 문자
            this.code in 0x0750..0x077F ||      // 아랍 확장 A
            this.code in 0xFB50..0xFDFF ||      //
            this.code in 0xFE70..0xFEFF

/**
 * Returns `true` if this character falls within a Thai Unicode block range.
 */
val Char.isThai: Boolean
    get() = this.code in 0x0E00..0x0E7F ||      // 타이 문자
            this.code in 0x1950..0x197F ||      // 타이 확장
            this.code in 0x1980..0x19DF ||      // 타이 확장 B
            this.code in 0x1A20..0x1AAF         // 타이 확장 추가

/**
 * Returns `true` if this character falls within a Korean (Hangul) Unicode block range.
 */
val Char.isKorean: Boolean
    get() = this.code in 0x1100..0x11FF ||      // 한글 자모
            this.code in 0x3130..0x318F ||      // 한글 호환 자모
            this.code in 0xA960..0xA97F ||      // 한글 자모 확장 A
            this.code in 0xAC00..0xD7AF ||      // 한글 글자 마디
            this.code in 0xD7B0..0xD7FF ||      // 한글 자모 확장 B
            this.code in 0xFFA0..0xFFDC         // 한글 반각

/**
 * Returns `true` if this character falls within a Japanese Unicode block range (Hiragana, Katakana,
 * or CJK supplement blocks). Note: kanji are shared with Chinese; sentence-level context is needed
 * to distinguish the two scripts reliably.
 */
val Char.isJapanese: Boolean
    get() = this.code in 0x3040..0x309F ||      // 히라가나 ひらがな
            this.code in 0x30A0..0x30FF ||      // 카타카나 カタカナ
            this.code in 0x31F0..0x31FF ||      // 카타카나 Phonetic 확장 カタカナ拡張
            this.code in 0xFF66..0xFF9F ||      // 일본어 반각 半角カタカナ
            this.code in 0x2E80..0x2EFF ||    // CJK Radicals Supplement
            this.code in 0x2F00..0x2FDF       // Kangxi Radicals

/**
 * Returns `true` if this character falls within a Chinese (CJK Unified Ideographs or Extension A)
 * Unicode block range.
 *
 * - `Char.code` is a UTF-16 code unit (max 0xFFFF), so supplementary-plane
 *   CJK Extension B–F ranges (0x20000..0x2EBEF) and the CJK Compatibility
 *   Supplement (0x2F800..0x2FA1F) can never match a single `Char` and are
 *   intentionally omitted. Use `String.codePoints()` to check those ranges.
 */
val Char.isChinese: Boolean
    get() = this.code in 0x4E00..0x9FFF ||      // 한자
            this.code in 0x3400..0x4DBF         // 한자 확장 A
