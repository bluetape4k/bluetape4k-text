package io.bluetape4k.tokenizer.korean.utils

import java.util.regex.Pattern

/**
 * twitter-text 라이브러리의 URL/해시태그/멘션/캐시태그 패턴을 인라인 정규식으로 대체한 내부 유틸리티입니다.
 *
 * ## 매치 범위 계약
 * - [VALID_URL]: `\(?`로 선행 `(`를 매치 범위에 포함한다. `(https://example.com)` 입력 시 `(https://example.com`을 매치한다.
 * - [VALID_HASHTAG], [VALID_MENTION_OR_LIST], [VALID_CASHTAG]:
 *   `group(1)`은 선행 문자(공백 또는 비단어 문자, 문자열 시작 시 빈 문자열), `group(2)`는 실제 토큰이다.
 *   `KoreanChunker.splitChunks`는 `splitBySpaceKeepingSpace` 후 세그먼트에서 실행되므로
 *   `group(1)`이 항상 비어 있어 `group(0) == group(2)`가 성립한다.
 */
internal object TwitterCompatPatterns {

    /**
     * 스킴(`https?://`, `ftp://`, `www.`) 또는 베어 도메인(`openkoreantext.org`)을 매치하는 URL 패턴입니다.
     *
     * - 선행 `(`는 `\(?`로 매치 범위에 포함 (`(https://...` 형태 지원)
     * - 베어 도메인은 `(?<!\S)` (공백/문자열 시작 뒤)로 제한
     * - 종결 문자 클래스에서 `)` 제외: `(https://example.com)` 에서 `)` 미포함
     */
    val VALID_URL: Pattern = Pattern.compile(
        """(\(?(?:https?://|ftp://|www\.)|(?<!\S)(?=[a-zA-Z0-9])(?:[a-zA-Z0-9\-]+\.)+[a-zA-Z]{2,})""" +
            """[a-zA-Z0-9\-._~:/?#\[\]@!${'$'}&'(*+,;=%-]*""",
        Pattern.UNICODE_CASE
    )

    /**
     * 해시태그 패턴입니다. `group(1)`은 선행 공백, `group(2)`는 `#태그`입니다.
     */
    val VALID_HASHTAG: Pattern = Pattern.compile(
        """(\s|^)(#[\p{L}\p{Digit}_]+)""",
        Pattern.UNICODE_CASE
    )

    /**
     * 멘션/리스트 패턴입니다. `group(1)`은 선행 문자(공백 포함 비단어 문자 또는 문자열 시작), `group(2)`는 `@계정`입니다.
     *
     * twitter-text `VALID_MENTION_PRECEDING_CHARS` 에 맞춰 `"`, `.`, `,` 등 뒤에서도 멘션을 인식한다.
     */
    val VALID_MENTION_OR_LIST: Pattern = Pattern.compile(
        """([^A-Za-z0-9_!#${'$'}%&*@＠]|^)(@[A-Za-z0-9_]+(?:/[A-Za-z0-9_]+)?)""",
        Pattern.UNICODE_CASE
    )

    /**
     * 캐시태그 패턴입니다. 대소문자를 허용하며, `group(1)`은 선행 공백, `group(2)`는 `$심볼`입니다.
     *
     * 예: `$AAPL`, `$twtr`, `$BRK.A`
     */
    val VALID_CASHTAG: Pattern = Pattern.compile(
        """(\s|^)(\$[A-Za-z]{1,6}(?:\.[A-Za-z]{1,2})?)"""
    )
}
