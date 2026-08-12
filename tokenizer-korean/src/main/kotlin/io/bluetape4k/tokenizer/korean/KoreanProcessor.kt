package io.bluetape4k.tokenizer.korean

import io.bluetape4k.logging.KLogging
import io.bluetape4k.tokenizer.korean.block.KoreanBlockwordProcessor
import io.bluetape4k.tokenizer.korean.normalizer.KoreanNormalizer
import io.bluetape4k.tokenizer.korean.phrase.KoreanPhrase
import io.bluetape4k.tokenizer.korean.phrase.KoreanPhraseExtractor
import io.bluetape4k.tokenizer.korean.phrase.NounPhraseExtractor
import io.bluetape4k.tokenizer.korean.stemmer.KoreanStemmer
import io.bluetape4k.tokenizer.korean.tokenizer.KoreanDetokenizer
import io.bluetape4k.tokenizer.korean.tokenizer.KoreanSentenceSplitter
import io.bluetape4k.tokenizer.korean.tokenizer.KoreanToken
import io.bluetape4k.tokenizer.korean.tokenizer.KoreanTokenizer
import io.bluetape4k.tokenizer.korean.tokenizer.NounTokenizer
import io.bluetape4k.tokenizer.korean.tokenizer.Sentence
import io.bluetape4k.tokenizer.korean.tokenizer.TokenizerProfile
import io.bluetape4k.tokenizer.korean.utils.KoreanDictionaryProvider
import io.bluetape4k.tokenizer.korean.utils.KoreanPos
import io.bluetape4k.tokenizer.model.BlockwordRequest
import io.bluetape4k.tokenizer.model.BlockwordResponse
import io.bluetape4k.tokenizer.model.Severity
import io.bluetape4k.tokenizer.model.Severity.HIGH
import io.bluetape4k.tokenizer.model.Severity.LOW
import io.bluetape4k.tokenizer.model.Severity.MIDDLE
import io.bluetape4k.tokenizer.model.requireTokenizeTextLength
import io.bluetape4k.tokenizer.utils.CharArraySet

/**
 * 한국어 토크나이저/정규화/구 추출/금칙어 처리를 묶은 퍼사드 API입니다.
 *
 * ## 동작/계약
 * - 각 기능은 `KoreanNormalizer`, `KoreanTokenizer`, `KoreanPhraseExtractor` 등 하위 유틸에 위임한다.
 * - 사전 추가/삭제 API는 `KoreanDictionaryProvider`의 런타임 사전을 직접 갱신한다.
 * - 금칙어 처리는 `KoreanBlockwordProcessor` 정책을 그대로 따른다.
 *
 * ```kotlin
 * val tokens = KoreanProcessor.tokenize("주말특가")
 * // tokens.map { it.text } == ["주말", "특가"]
 * ```
 */
object KoreanProcessor: KLogging() {

    /**
     * 한국어 토크나이저가 사용하는 사전을 호출 코루틴을 차단하지 않고 미리 로드합니다.
     *
     * 애플리케이션 startup/readiness 단계에서 호출하면 첫 동기 facade 조회의 IO blocking을
     * 요청 경로 밖으로 이동할 수 있습니다. 실제 loader lifecycle과 취소·재시도 규칙은
     * [KoreanDictionaryProvider]가 소유합니다.
     */
    suspend fun preload() {
        KoreanDictionaryProvider.preload()
    }

    /**
     * 구어체/반복 문자/오타를 정규화합니다.
     *
     * ## 동작/계약
     * - 정규화기 호출 전에 최대 입력 길이를 검증한다.
     * - 내부 구현은 `KoreanNormalizer.normalize`를 그대로 위임한다.
     * - 빈 입력/비한글 구간 처리는 정규화기 구현 규칙을 따른다.
     *
     * ```kotlin
     * val normalized = KoreanProcessor.normalize("안됔ㅋㅋㅋㅋ")
     * // normalized == "안돼ㅋㅋㅋ"
     * ```
     *
     * @param text 정규화할 입력 문자열입니다.
     * @return 구어체 반복, 오타, 받침 변형을 보정한 문자열입니다.
     */
    fun normalize(text: CharSequence): CharSequence {
        requireTokenizeTextLength(text)
        return KoreanNormalizer.normalize(text)
    }

    /**
     * 문장을 1-best 형태소 토큰 리스트로 분석합니다.
     *
     * ## 동작/계약
     * - 토크나이저 호출 전에 최대 입력 길이를 검증한다.
     * - `KoreanTokenizer.tokenize(text, profile)`를 그대로 위임한다.
     * - 결과 토큰에는 필요 시 용언 `stem` 정보가 포함된다.
     *
     * ```kotlin
     * val tokens = KoreanProcessor.tokenize("주말특가")
     * // tokens.map { it.text } == ["주말", "특가"]
     * ```
     *
     * @param text 분석할 입력 문자열입니다.
     * @param profile 토큰화 비용과 후보 탐색 폭을 제어하는 [TokenizerProfile]입니다.
     * @return 1-best 형태소 [KoreanToken] list입니다.
     */
    fun tokenize(
        text: CharSequence,
        profile: TokenizerProfile = TokenizerProfile.DefaultProfile,
    ): List<KoreanToken> {
        requireTokenizeTextLength(text)
        return KoreanTokenizer.tokenize(text, profile)
    }

    /**
     * 명사 중심 규칙으로 문장을 분석합니다.
     *
     * ## 동작/계약
     * - 토크나이저 호출 전에 최대 입력 길이를 검증한다.
     * - `NounTokenizer.tokenize(text, profile)`를 위임 호출한다.
     * - phrase 추출 전처리용 명사 토큰화 경로로 사용된다.
     *
     * ```kotlin
     * val tokens = KoreanProcessor.tokenizeForNoun("떡 만두국")
     * // tokens.isNotEmpty() == true
     * ```
     *
     * @param text 명사 중심으로 분석할 입력 문자열입니다.
     * @param profile 토큰화 비용과 후보 탐색 폭을 제어하는 [TokenizerProfile]입니다.
     * @return 명사 구 추출 전처리에 쓰는 [KoreanToken] list입니다.
     */
    fun tokenizeForNoun(
        text: CharSequence,
        profile: TokenizerProfile = TokenizerProfile.DefaultProfile,
    ): List<KoreanToken> {
        requireTokenizeTextLength(text)
        return NounTokenizer.tokenize(text, profile)
    }

    /**
     * 문장을 청크별 상위 `n` 후보로 분석합니다.
     *
     * ## 동작/계약
     * - 토크나이저 호출 전에 최대 입력 길이를 검증한다.
     * - `KoreanTokenizer.tokenizeTopN(text, n, profile)`를 그대로 위임한다.
     *
     * ```kotlin
     * val top = KoreanProcessor.tokenizeTopN("가느다란", n = 1)
     * // top.isNotEmpty() == true
     * ```
     *
     * @param text 후보를 추출할 입력 문자열입니다.
     * @param n 각 청크에서 유지할 상위 후보 수입니다.
     * @param profile 후보 탐색 폭과 점수 계산 방식을 제어하는 [TokenizerProfile]입니다.
     * @return 청크별 상위 후보 token path list입니다.
     */
    fun tokenizeTopN(
        text: CharSequence,
        n: Int = 1,
        profile: TokenizerProfile = TokenizerProfile.DefaultProfile,
    ): List<List<List<KoreanToken>>> {
        requireTokenizeTextLength(text)
        return KoreanTokenizer.tokenizeTopN(text, n, profile)
    }

    /**
     * 명사 사전에 단어 목록을 추가합니다.
     *
     * ## 동작/계약
     * - `KoreanDictionaryProvider.addWordsToDictionary(Noun, words)`를 호출한다.
     *
     * ```kotlin
     * KoreanProcessor.addNounsToDictionary(listOf("후랴오교"))
     * // KoreanDictionaryProvider.koreanDictionary.getValue(KoreanPos.Noun).contains("후랴오교") == true
     * ```
     *
     * @param words 런타임 명사 사전에 추가할 단어 목록입니다.
     */
    fun addNounsToDictionary(words: List<String>) {
        KoreanDictionaryProvider.addWordsToDictionary(KoreanPos.Noun, words)
    }

    /**
     * 명사 사전에 가변 인자 단어를 추가합니다.
     *
     * ## 동작/계약
     * - `addWordsToDictionary(Noun, *words)`를 호출한다.
     *
     * ```kotlin
     * KoreanProcessor.addNounsToDictionary("주말특가")
     * // KoreanDictionaryProvider.koreanDictionary.getValue(KoreanPos.Noun).contains("주말특가") == true
     * ```
     *
     * @param words 런타임 명사 사전에 추가할 단어들입니다.
     */
    fun addNounsToDictionary(vararg words: String) {
        KoreanDictionaryProvider.addWordsToDictionary(KoreanPos.Noun, *words)
    }


    /**
     * 심각도별 금칙어 사전에 단어를 추가합니다.
     *
     * ## 동작/계약
     * - `severity` source tier에 단어를 추가한다.
     * - threshold 조회 view에서는 LOW source는 LOW, MIDDLE source는 LOW/MIDDLE,
     *   HIGH source는 LOW/MIDDLE/HIGH에서 조회된다.
     * - 복합 명사 탐지를 위해 동일 단어를 명사 사전과 `properNouns`에도 추가한다.
     *
     * ```kotlin
     * KoreanProcessor.addBlockwords(listOf("분수쑈"), Severity.HIGH)
     * // KoreanDictionaryProvider.blockWords.getValue(Severity.HIGH).contains("분수쑈") == true
     * ```
     *
     * @param words 금칙어 사전에 추가할 단어 목록입니다.
     * @param severity 추가할 금칙어 심각도입니다.
     */
    fun addBlockwords(
        words: List<String>,
        severity: Severity = Severity.DEFAULT,
    ) {
        withBlockwordDictionary(severity) {
            addAll(words)
        }
        // 복합명사의 경우 등록되지 않으면 형태소 분석을 못한다 (예: 분수쑈 -> `분수 + 쑈` 로 분석하면 `분수쑈` 라는 금칙어를 처리할 수 없다)
        addNounsToDictionary(words)
        KoreanDictionaryProvider.properNouns.addAll(words)
    }

    /**
     * 금칙어 사전에서 단어를 제거합니다. (Deprecated)
     *
     * ## 동작/계약
     * - `removeBlockwords(words, severity)`와 동일하게 해당 source tier에서 제거한다.
     *
     * ```kotlin
     * KoreanProcessor.removeBlockword(listOf("금칙어"), Severity.HIGH)
     * // deprecated
     * ```
     *
     * @param words 제거할 금칙어 목록입니다.
     * @param severity 제거할 금칙어 심각도입니다.
     */
    @Deprecated("Use removeBlockwords instead", replaceWith = ReplaceWith("removeBlockwords(words, severity)"))
    fun removeBlockword(
        words: List<String>,
        severity: Severity = Severity.DEFAULT,
    ) {
        withBlockwordDictionary(severity) {
            removeAll(words)
        }
    }

    /**
     * [addBlockwords]가 기록하는 모든 사전에서 단어를 제거합니다.
     *
     * ## 동작/계약
     * - 지정한 [severity] source tier에서 제거한다.
     * - [addBlockwords]와 대칭이 되도록 [KoreanDictionaryProvider.koreanDictionary]의 `Noun` 항목과
     *   [KoreanDictionaryProvider.properNouns]에서도 제거한다.
     *
     * ```kotlin
     * KoreanProcessor.removeBlockwords(listOf("금칙어"), Severity.HIGH)
     * // blockWords, Noun dictionary, properNouns에서 제거됨
     * ```
     *
     * @param words 제거할 금칙어 목록입니다.
     * @param severity 제거할 금칙어 심각도입니다.
     */
    fun removeBlockwords(
        words: List<String>,
        severity: Severity = Severity.DEFAULT,
    ) {
        withBlockwordDictionary(severity) {
            removeAll(words)
        }
        KoreanDictionaryProvider.removeWordsFromDictionary(KoreanPos.Noun, words)
        KoreanDictionaryProvider.properNouns.removeAll(words)
    }

    /**
     * 지정한 source tier의 금칙어 사전을 비웁니다.
     *
     * ## 동작/계약
     * - 해당 tier의 단어만 제거하며, threshold cumulative view는 남은 higher tier 단어를 계속 포함한다.
     *
     * ```kotlin
     * KoreanProcessor.clearBlockwords(Severity.HIGH)
     * // high dictionary cleared
     * ```
     *
     * @param severity 비울 금칙어 source tier입니다.
     */
    fun clearBlockwords(severity: Severity = Severity.DEFAULT) {
        withBlockwordDictionary(severity) {
            val changed = isNotEmpty()
            clear()
            changed
        }
    }

    private inline fun withBlockwordDictionary(
        severity: Severity,
        action: CharArraySet.() -> Boolean,
    ) {
        KoreanDictionaryProvider.mutateBlockwords(severity, action)
    }

    /**
     * 토큰 리스트에서 공백 토큰을 제외한 텍스트 목록을 반환합니다.
     *
     * ## 동작/계약
     * - `KoreanPos.Space` 토큰은 제거하고 나머지 `text`만 추출한다.
     *
     * ```kotlin
     * val words = KoreanProcessor.tokensToStrings(listOf(KoreanToken("안녕", KoreanPos.Noun, 0, 2)))
     * // words == ["안녕"]
     * ```
     *
     * @param tokens 문자열로 바꿀 [KoreanToken] list입니다.
     * @return 공백 token을 제외한 token text list입니다.
     */
    fun tokensToStrings(tokens: List<KoreanToken>): List<String> =
        tokens.filterNot { it.pos == KoreanPos.Space }.map { it.text }

    /**
     * 문장을 `Sentence` 시퀀스로 분리합니다.
     *
     * ## 동작/계약
     * - 문장 분리기 호출 전에 최대 입력 길이를 검증한다.
     * - `KoreanSentenceSplitter.split(text)`를 그대로 위임한다.
     *
     * ```kotlin
     * val sentences = KoreanProcessor.splitSentences("안녕? 세상아?").toList()
     * // sentences.size == 2
     * ```
     *
     * @param text 문장 단위로 분리할 입력 문자열입니다.
     * @return 입력 문자열에서 추출한 [Sentence] sequence입니다.
     */
    fun splitSentences(text: CharSequence): Sequence<Sentence> {
        requireTokenizeTextLength(text)
        return KoreanSentenceSplitter.split(text)
    }

    /**
     * 토큰 목록에서 phrase를 추출합니다.
     *
     * ## 동작/계약
     * - `KoreanPhraseExtractor.extractPhrases(tokens, filterSpam, enableHashtags)`를 위임 호출한다.
     *
     * ```kotlin
     * val phrases = KoreanProcessor.extractPhrases(KoreanProcessor.tokenize("성탄절 쇼핑"), filterSpam = false)
     * // phrases.isNotEmpty() == true
     * ```
     *
     * @param tokens phrase 추출에 사용할 형태소 token list입니다.
     * @param filterSpam `true`이면 spam으로 판단되는 phrase를 결과에서 제외합니다.
     * @param enableHashtags `true`이면 hashtag token을 phrase 후보에 포함합니다.
     * @return 추출한 [KoreanPhrase] list입니다.
     */
    fun extractPhrases(
        tokens: List<KoreanToken>,
        filterSpam: Boolean = false,
        enableHashtags: Boolean = true,
    ): List<KoreanPhrase> {
        return KoreanPhraseExtractor.extractPhrases(tokens, filterSpam, enableHashtags)
    }


    /**
     * 명사 중심 토큰에서 phrase를 추출합니다.
     *
     * ## 동작/계약
     * - `NounPhraseExtractor.extractPhrases(tokens)`를 그대로 위임한다.
     *
     * ```kotlin
     * val phrases = KoreanProcessor.extractPhrasesForNoun(KoreanProcessor.tokenizeForNoun("떡 만두국"))
     * // phrases.map { it.text }.contains("만두국") == true
     * ```
     *
     * @param tokens 명사 중심 토큰화로 얻은 [KoreanToken] list입니다.
     * @return 명사 phrase [KoreanPhrase] list입니다.
     */
    fun extractPhrasesForNoun(tokens: List<KoreanToken>): List<KoreanPhrase> {
        return NounPhraseExtractor.extractPhrases(tokens)
    }

    /**
     * 토큰 리스트의 어미 병합 및 용언 원형 복원을 수행합니다.
     *
     * ## 동작/계약
     * - `KoreanStemmer.stem(tokens)`를 그대로 위임한다.
     *
     * ```kotlin
     * val stemmed = KoreanProcessor.stem(KoreanProcessor.tokenize("가느다란"))
     * // stemmed.first().stem == "갈다"
     * ```
     *
     * @param tokens stemming 대상 [KoreanToken] list입니다.
     * @return 어미 병합과 용언 원형 복원을 적용한 [KoreanToken] list입니다.
     */
    fun stem(tokens: List<KoreanToken>): List<KoreanToken> {
        return KoreanStemmer.stem(tokens)
    }


    /**
     * 토큰 문자열 목록을 문장 문자열로 복원합니다.
     *
     * ## 동작/계약
     * - 토큰 문자열 총 길이를 먼저 검증해 과도한 병합 문자열 할당을 막는다.
     * - `KoreanDetokenizer.detokenize(tokens)`를 그대로 위임한다.
     *
     * ```kotlin
     * val text = KoreanProcessor.detokenize(listOf("뭐", "완벽", "하진", "않", "지만"))
     * // text == "뭐 완벽하진 않지만"
     * ```
     *
     * @param tokens 문장 문자열로 병합할 token 문자열 collection입니다.
     * @return 한국어 띄어쓰기 규칙을 적용해 복원한 문장 문자열입니다.
     */
    fun detokenize(tokens: Collection<String>): String {
        requireTokenizeTextLength(tokens.sumOf { it.length })
        return KoreanDetokenizer.detokenize(tokens)
    }

    /**
     * 요청 옵션에 따라 금칙어를 마스킹합니다.
     *
     * ## 동작/계약
     * - `KoreanBlockwordProcessor.maskBlockwords(request)`를 그대로 위임한다.
     *
     * ```kotlin
     * val response = KoreanProcessor.maskBlockwords(BlockwordRequest("미니미와 니미"))
     * // response.text.contains("**")
     * ```
     *
     * @param request 금칙어 마스킹 입력 문자열과 옵션입니다.
     * @return 금칙어 마스킹 결과 [BlockwordResponse]입니다.
     */
    fun maskBlockwords(request: BlockwordRequest): BlockwordResponse {
        return KoreanBlockwordProcessor.maskBlockwords(request)
    }
}
