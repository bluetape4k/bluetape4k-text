package io.bluetape4k.lingua

/**
 * 문자가 ASCII 문자(`0..127` 코드 포인트)인지 확인합니다.
 */
val Char.isAscii: Boolean get() = this.code in 0..127

/**
 * 문자가 라틴 유니코드 블록 범위에 속하는지 확인합니다.
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
 * 문자가 아랍어 유니코드 블록 범위에 속하는지 확인합니다.
 */
val Char.isArabic: Boolean
    get() = this.code in 0x0600..0x06FF ||      // 아랍 문자
            this.code in 0x0750..0x077F ||      // 아랍 확장 A
            this.code in 0xFB50..0xFDFF ||      // 아랍어 표현형 A
            this.code in 0xFE70..0xFEFF

/**
 * 문자가 태국어 유니코드 블록 범위에 속하는지 확인합니다.
 */
val Char.isThai: Boolean
    get() = this.code in 0x0E00..0x0E7F ||      // 타이 문자
            this.code in 0x1950..0x197F ||      // 타이 확장
            this.code in 0x1980..0x19DF ||      // 타이 확장 B
            this.code in 0x1A20..0x1AAF         // 타이 확장 추가

/**
 * 문자가 한국어(한글) 유니코드 블록 범위에 속하는지 확인합니다.
 */
val Char.isKorean: Boolean
    get() = this.code in 0x1100..0x11FF ||      // 한글 자모
            this.code in 0x3130..0x318F ||      // 한글 호환 자모
            this.code in 0xA960..0xA97F ||      // 한글 자모 확장 A
            this.code in 0xAC00..0xD7AF ||      // 한글 글자 마디
            this.code in 0xD7B0..0xD7FF ||      // 한글 자모 확장 B
            this.code in 0xFFA0..0xFFDC         // 한글 반각

/**
 * 문자가 일본어 유니코드 블록 범위에 속하는지 확인합니다.
 *
 * 히라가나, 가타카나, CJK 보조 블록을 포함합니다. 한자는 중국어와 공유되므로 두 문자를 안정적으로 구분하려면 문장 수준 문맥이 필요합니다.
 */
val Char.isJapanese: Boolean
    get() = this.code in 0x3040..0x309F ||      // 히라가나 ひらがな
            this.code in 0x30A0..0x30FF ||      // 카타카나 カタカナ
            this.code in 0x31F0..0x31FF ||      // 카타카나 음성 확장 カタカナ拡張
            this.code in 0xFF66..0xFF9F ||      // 일본어 반각 半角カタカナ
            this.code in 0x2E80..0x2EFF ||      // CJK 부수 보조
            this.code in 0x2F00..0x2FDF         // 강희자전 부수

/**
 * 문자가 중국어(CJK 통합 한자 또는 확장 A) 유니코드 블록 범위에 속하는지 확인합니다.
 *
 * - `Char.code`는 UTF-16 코드 단위(최대 0xFFFF)이므로 supplementary plane의
 *   확장 한자 범위인 CJK 확장 B-F(0x20000..0x2EBEF)와 CJK 호환 보조 범위(0x2F800..0x2FA1F)는
 *   단일 `Char`와 매치될 수 없다. 해당 범위 확인에는 `String.codePoints()`를 사용해야 한다.
 */
val Char.isChinese: Boolean
    get() = this.code in 0x4E00..0x9FFF ||      // 한자
            this.code in 0x3400..0x4DBF         // 한자 확장 A
