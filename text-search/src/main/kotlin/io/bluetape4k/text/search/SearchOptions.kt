package io.bluetape4k.text.search

import java.io.Serializable

/**
 * Aho-Corasick matching에 사용할 단어 경계 감지 모드입니다.
 *
 * - [NONE]: substring match를 허용합니다. 경계를 강제하지 않습니다.
 * - [LATIN_ALPHA]: [Character.isAlphabetic] 경계에서만 match합니다. 한국어/CJK 문자열에는 [WHITESPACE_SEPARATED]를 우선합니다.
 * - [WHITESPACE_SEPARATED]: 공백 경계에서만 match합니다.
 */
enum class WordBoundary {
    /** 경계가 없습니다. Substring match를 허용합니다. */
    NONE,

    /**
     * [Character.isAlphabetic] 기반 알파벳 경계입니다.
     * **참고**: [Character.isAlphabetic]은 CJK ideograph와 Hangul도 포함하므로 한국어 문자열에는 [WHITESPACE_SEPARATED]를 사용하세요.
     */
    LATIN_ALPHA,

    /** 공백 경계입니다. [Character.isWhitespace]로 구분된 token만 match합니다. */
    WHITESPACE_SEPARATED,
}

/**
 * 매칭 전에 키워드와 검색 문자열에 모두 적용할 유니코드 정규화 형식입니다.
 *
 * - [NONE]: 정규화를 적용하지 않습니다. 기본값이며 ASCII-only 문자열에 적합합니다.
 * - [NFC]: Canonical Decomposition 뒤 Canonical Composition을 적용합니다. Hangul jamo composition에 유용합니다.
 * - [NFKC]: Compatibility Decomposition 뒤 Canonical Composition을 적용합니다. Fullwidth char, ligature 등을 정규화합니다.
 */
enum class NormalizationForm {
    /** 정규화를 적용하지 않습니다. */
    NONE,

    /**
     * NFC(Canonical Decomposition + Canonical Composition)입니다.
     * 예: decomposed jamo `ㄴㅏ` -> composed `나`.
     */
    NFC,

    /**
     * NFKC(Compatibility Decomposition + Canonical Composition)입니다.
     * 예: `㈜` -> `(주)`. 길이가 바뀔 수 있으며 [io.bluetape4k.text.search.internal.OffsetMapping]이 자동 처리합니다.
     */
    NFKC,
}

/**
 * [AhoCorasickAutomaton] 검색 옵션입니다. 모든 field는 불변이며 변형 값을 만들 때는 [copy]를 사용합니다.
 *
 * ```kotlin
 * val opts = SearchOptions(ignoreCase = true, wordBoundary = WordBoundary.WHITESPACE_SEPARATED)
 * val opts2 = opts.copy(allowOverlaps = false)
 * ```
 *
 * **참고**: [stopOnFirstMatch]는 [io.bluetape4k.text.search.flow.matchesAsFlow]에서 무시됩니다.
 * 첫 match 뒤 중단하려면 Flow에 `take(1)`을 사용하세요.
 */
data class SearchOptions(
    /** 매칭 중 대소문자를 무시할지 여부입니다. 기본값은 `false`입니다. */
    val ignoreCase: Boolean = false,

    /** 겹치는 match를 허용할지 여부입니다. 기본값은 `true`입니다. `false`이면 더 긴 keyword가 우선합니다. */
    val allowOverlaps: Boolean = true,

    /** 단어 경계 감지 mode입니다. 기본값은 [WordBoundary.NONE]입니다. */
    val wordBoundary: WordBoundary = WordBoundary.NONE,

    /** 유니코드 정규화 형식입니다. 기본값은 [NormalizationForm.NONE]입니다. */
    val normalization: NormalizationForm = NormalizationForm.NONE,

    /**
     * 첫 match를 찾은 뒤 중단할지 여부입니다. 기본값은 `false`입니다.
     * **참고**: [io.bluetape4k.text.search.flow.matchesAsFlow]에서는 무시됩니다.
     */
    val stopOnFirstMatch: Boolean = false,
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}
