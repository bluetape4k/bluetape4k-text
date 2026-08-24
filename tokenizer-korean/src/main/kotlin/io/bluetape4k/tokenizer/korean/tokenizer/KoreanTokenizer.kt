package io.bluetape4k.tokenizer.korean.tokenizer

import io.bluetape4k.collections.eclipse.multi.listMultimapOf
import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.requireNotNull
import io.bluetape4k.logging.error
import io.bluetape4k.logging.trace
import io.bluetape4k.tokenizer.exceptions.TokenizerException
import io.bluetape4k.tokenizer.korean.stemmer.KoreanStemmer
import io.bluetape4k.tokenizer.korean.utils.KoreanDictionaryProvider
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Adjective
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Adverb
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Conjunction
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Exclamation
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Josa
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Korean
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Noun
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Unknown
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Verb
import io.bluetape4k.tokenizer.korean.utils.KoreanPos
import io.bluetape4k.tokenizer.korean.utils.KoreanPosx
import io.bluetape4k.tokenizer.korean.utils.KoreanSubstantive
import io.bluetape4k.tokenizer.model.requireTokenizeTextLength
import org.eclipse.collections.api.multimap.MutableMultimap

/**
 * 한국어 문장을 형태소 토큰 열로 분석하는 기본 토크나이저입니다.
 *
 * ## 동작/계약
 * - 입력은 먼저 `KoreanChunker.chunk`로 청크 분할되고, `Korean` 품사 청크만 동적 계획법 파서를 적용한다.
 * - 후보 분석 결과는 각 청크별로 최대 `topN`개를 반환하고, `KoreanSubstantive.collapseNouns`로 명사 연쇄를 보정한다.
 * - 최종 `tokenize`는 `KoreanStemmer.stem`을 거쳐 용언 원형 정보를 채운다.
 *
 * ```kotlin
 * val tokens = KoreanTokenizer.tokenize("엄청작아서귀엽다")
 * // tokens == [엄청(Adverb), 작아서(Adjective, stem=작다), 귀엽다(Adjective, stem=귀엽다)]
 * ```
 */
object KoreanTokenizer: KLogging() {

    private const val TOP_N_PER_STATE = 5
    private const val MAX_TRACE_BACK = 8

    /**
     * 0은 선택, 1은 필수를 뜻합니다.
     * *는 선택 반복, +는 필수 반복을 뜻합니다.
     *
     * 체언: 초거대기업의
     * 용언: 하였었습니다, 개예뻤었다
     * 수식언(Modifier): 모르는, 할수도있는, 보이기도하는, 예뻐, 예쁜, 완전, 레알, 초인간적인, 잘, 잘한
     * 독립언: 문장 안에서 독립적으로 쓰이는 품사
     * 관계언: 조사
     *
     * `N`/`Noun`: 명사(대명사, 회사명, 고유명사, 인명, 수사, 독립/의존 명사 포함)
     * `V`/`Verb`: 동사(하, 먹, 자, 차)
     * `J`/`Adjective`: 형용사(예쁘다, 크다, 작다)
     * `A`/`Adverb`: 부사(잘, 매우, 빨리, 반드시, 과연)
     * `D`/`Determiner`: 관형사(새, 헌, 참, 첫, 이, 그, 저)
     * `E`/`Exclamation`: 감탄사(헐, ㅋㅋㅋ, 어머나, 얼씨구)
     *
     * `C`/`Conjunction`: 접속사
     *
     * `j`/`SubstantiveJosa`: 조사(의, 에, 에서)
     * `l`/`AdverbialJosa`: 부사격 조사(~인, ~의, ~일)
     * `e`/`Eomi`: 어말어미(다, 요, 여, 하댘ㅋㅋ)
     * `r`/`PreEomi`: 선어말어미(었)
     *
     * `m`/`Modifier`: 관형사('초'대박)
     * `v`/`VerbPrefix`: 동사 접두어('쳐'먹어)
     * `s`/`Suffix`: 접미사(~적)
     */
    private val sequenceDefinition = mapOf(
        // 체언
        "D0m*N1s0j0" to Noun,
        // 용언: 초기뻐하다, 와주세요, 초기뻤었고, 추첨하다, 구경하기힘들다, 기뻐하는, 기쁜, 추첨해서, 좋아하다, 걸려있을
        "v*V1r*e0" to Verb,
        "v*J1r*e0" to Adjective,
        // 수식언: 부사
        "A1" to Adverb,
        // 독립언
        "C1" to Conjunction,
        "E+" to Exclamation,
        "j1" to Josa
    )

    private val koreanPosTrie by lazy {
        KoreanPosx.getTrie(sequenceDefinition)
    }

    /**
     * 문장을 1-best 분석으로 형태소 분해하고 원형(stem)을 보정한 토큰 리스트를 반환합니다.
     *
     * ## 동작/계약
     * - `tokenizeTopN(text, 1, profile)` 결과에서 각 청크의 첫 번째 후보만 사용한다.
     * - 비한글 청크(`Space`, `Punctuation` 등)는 원래 청크 토큰을 그대로 유지한다.
     * - `KoreanTokenizerTest` 기준으로 `"주말특가"`는 `[주말(Noun), 특가(Noun)]`로 분해된다.
     *
     * ```kotlin
     * val tokens = KoreanTokenizer.tokenize("야이건뭐")
     * // tokens == [야(Exclamation), 이건(Noun), 뭐(Noun)]
     * ```
     */
    fun tokenize(
        text: CharSequence,
        profile: TokenizerProfile = TokenizerProfile.DefaultProfile,
    ): List<KoreanToken> {
        return tokenizeWithDictionary(
            text,
            profile,
            KoreanDictionaryProvider.currentDictionarySnapshot().value,
        )
    }

    /** 금칙어 aggregate snapshot에서 전달한 immutable 명사 사전으로 토큰화합니다. */
    internal fun tokenize(
        text: CharSequence,
        profile: TokenizerProfile = TokenizerProfile.DefaultProfile,
        dictionary: Map<KoreanPos, Set<String>>,
    ): List<KoreanToken> = tokenizeWithDictionary(text, profile, dictionary)

    private fun tokenizeWithDictionary(
        text: CharSequence,
        profile: TokenizerProfile,
        dictionary: Map<KoreanPos, Set<String>>,
    ): List<KoreanToken> {
        requireTokenizeTextLength(text)
        try {
            val tokenized = tokenizeTopNWithDictionary(text, 1, profile, dictionary)
                .flatMap { it.firstOrNull() ?: emptyList() }

            return KoreanStemmer.stem(tokenized)
        } catch (e: Exception) {
            log.error(e) { "Error tokenizing a chunk. textLength=${text.length}" }
            throw TokenizerException("Error tokenizing a chunk. textLength=${text.length}", e)
        }
    }

    /**
     * 문장을 청크별 상위 `topN` 후보 분석 결과로 반환합니다.
     *
     * ## 동작/계약
     * - 반환 타입은 `List<청크, List<후보, List<KoreanToken>>>` 구조다.
     * - `topN`은 1 이상이어야 하며, 0 이하를 전달하면 `IllegalArgumentException`을 던진다.
     * - 파싱 중 예외가 발생하면 원문 대신 입력 길이만 포함한 `TokenizerException`으로 감싸서 던진다.
     * - `KoreanTokenizerTest`에서 사용자 사전 추가 전/후 결과가 달라지는 경로는 이 함수의 후보 생성 결과를 따른다.
     *
     * ```kotlin
     * val top = KoreanTokenizer.tokenizeTopN("가느다란", topN = 1)
     * // top.isNotEmpty() == true
     * ```
     */
    fun tokenizeTopN(
        text: CharSequence,
        topN: Int = 1,
        profile: TokenizerProfile = TokenizerProfile.DefaultProfile,
    ): List<List<List<KoreanToken>>> {
        requireTokenizeTextLength(text)
        require(topN >= 1) { "topN must be greater than or equal to 1. topN=$topN" }

        try {
            val dictionary = KoreanDictionaryProvider.currentDictionarySnapshot().value
            return tokenizeTopNWithDictionary(text, topN, profile, dictionary)
        } catch (e: Exception) {
            log.error(e) { "Error tokenizing a chunk. textLength=${text.length}" }
            throw TokenizerException("Error tokenizing a chunk. textLength=${text.length}", e)
        }
    }

    private fun tokenizeTopNWithDictionary(
        text: CharSequence,
        topN: Int,
        profile: TokenizerProfile,
        dictionary: Map<KoreanPos, Set<String>>,
    ): List<List<List<KoreanToken>>> = KoreanChunker.chunk(text).map {
        when (it.pos) {
            Korean -> {
                // 각 청크의 최적 분석 후보를 구합니다.
                val parsed = parseKoreanChunk(it, profile, topN, dictionary)

                // 한 글자 명사가 이어진 구간을 하나의 unknown 명사로 접습니다: (가Noun 회Noun -> 가회Noun*)
                parsed.map(KoreanSubstantive::collapseNouns)
            }

            else -> listOf(listOf(it))
        }
    }

    /**
     * 동적 계획법으로 [chunk]의 상위 분석 후보를 구합니다.
     *
     * @param chunk 전체가 `Korean` 품사인 입력 청크입니다. 호출자가 청크 유효성을 보장하므로 성능을 위해 반복 검증하지 않습니다.
     * @param profile 토큰 점수 계산과 `spaceGuide` 적용 방식을 제어하는 프로필입니다.
     * @param topN 반환할 상위 후보 개수입니다.
     * @param dictionary 이번 tokenize 호출 전체에서 사용할 immutable 사전 snapshot입니다.
     * @return 점수순으로 고른 상위 후보별 토큰 경로 목록입니다.
     */
    private fun parseKoreanChunk(
        chunk: KoreanToken,
        profile: TokenizerProfile = TokenizerProfile.DefaultProfile,
        topN: Int = 1,
        dictionary: Map<KoreanPos, Set<String>>,
    ): List<List<KoreanToken>> {
        return findTopCandidates(chunk, profile, dictionary).take(topN)
    }

    private fun findTopCandidates(
        chunk: KoreanToken,
        profile: TokenizerProfile,
        dictionary: Map<KoreanPos, Set<String>>,
    ): List<List<KoreanToken>> {
        val directMatch = findDirectMatch(chunk, dictionary)
        val nounDictionary = dictionary[Noun]

        // 위치별 후보 해석을 저장하는 버퍼입니다.
        val candidateParse = CandidateParse(
            parse = ParsedChunk(listOf(), 1, profile),
            curTrie = koreanPosTrie,
            ending = null
        )
        val solutions = listMultimapOf<Int, CandidateParse>()
            .apply {
                put(0, candidateParse)
            }
        // val solutions = hashMapOf(0 to candidateParse)

        // 각 위치 상태마다 상위 N개 후보를 유지합니다.
        for (end in 1..chunk.length) {
            for (start in end - 1 downTo maxOf(end - MAX_TRACE_BACK, 0)) {

                val word = chunk.text.slice(start until end)

                // 청크 처리가 진행되며 더 이상 참조하지 않는 후보를 제거합니다.
                removeUnusedSolutions(start, end, solutions)

                val curSolutions =
                    solutions[start].requireNotNull("solutions[start=$start] in chunkLength=${chunk.length}")

                val candidates: List<CandidateParse> = curSolutions.flatMap { solution: CandidateParse ->
                    val possiblePoses: List<PossibleTrie> = solution.ending
                        ?.let {
                            solution.curTrie.map { PossibleTrie(it, 0) } + koreanPosTrie.map { PossibleTrie(it, 1) }
                        }
                        ?: solution.curTrie.map { PossibleTrie(it, 0) }

                    possiblePoses
                        .filter {
                            it.curTrie.curPos == Noun ||
                                    (dictionary[it.curTrie.curPos]?.contains(word) == true)
                        }
                        .map { t: PossibleTrie ->
                            val candidateToAdd =
                                if (t.curTrie.curPos == Noun && nounDictionary?.contains(word) == false) {
                                    val isWordName: Boolean = KoreanSubstantive.isName(word)
                                    val isKoreanNumber = KoreanSubstantive.isKoreanNumber(word)
                                    val isWordKoreanNameVariation = KoreanSubstantive.isKoreanNameVariation(word)

                                    val unknown = !(isWordName || isKoreanNumber || isWordKoreanNameVariation)
                                    val pos = Noun

                                    val token =
                                        KoreanToken(word, pos, chunk.offset + start, word.length, unknown = unknown)
                                    ParsedChunk(listOf(token), t.words, profile)
                                } else {
                                    val pos = t.curTrie.curPos ?: Unknown
                                    val token = KoreanToken(word, pos, chunk.offset + start, word.length)
                                    ParsedChunk(listOf(token), t.words, profile)
                                }
                            val nextTrie = t.curTrie.nextTrie
                                ?.map { if (it == KoreanPosx.SelfNode) t.curTrie else it }
                                ?.toList()
                                ?: emptyList()

                            CandidateParse(solution.parse + candidateToAdd, nextTrie, t.curTrie.ending)
                        }
                }

                val currentSolutions = solutions[end] ?: emptyList()

                val parses = (currentSolutions + candidates)
                    .sortedWith(compareBy({ it.parse.score }, { it.parse.posTieBreaker }))
                    .take(TOP_N_PER_STATE)

                solutions.removeAll(end)
                solutions.putAll(end, parses)
            }
        }

        val finalSolutions =
            solutions[chunk.length].requireNotNull("solutions[chunk.length=${chunk.length}]")
        val topCandidates =
            if (finalSolutions.isEmpty()) {
                val token = KoreanToken(chunk.text, Noun, chunk.offset, chunk.length, unknown = true)
                listOf(listOf(token))
            } else {
                finalSolutions.sortedBy { it.parse.score }.map { it.parse.posNodes }
            }

        return (directMatch + topCandidates).distinct()
    }

    private fun removeUnusedSolutions(
        start: Int,
        end: Int,
        solutions: MutableMultimap<Int, CandidateParse>,
    ): MutableMultimap<Int, CandidateParse> {
        // 사용하지 않는 후보 객체 참조가 solutions에 남지 않게 정리합니다.
        if (end > MAX_TRACE_BACK && start + 1 == end) {
            log.trace { "remove solution. index=${end - MAX_TRACE_BACK - 1}" }
            solutions.removeAll(end - MAX_TRACE_BACK - 1)
        }
        return solutions
    }

    private fun findDirectMatch(
        chunk: KoreanToken,
        dictionary: Map<KoreanPos, Set<String>>,
    ): List<List<KoreanToken>> {
        log.trace { "직접 매치 탐색. offset=${chunk.offset}, length=${chunk.length}, pos=${chunk.pos}" }
        return dictionary.entries
            .firstOrNull { (_, dict) ->
                dict.contains(chunk.text)
            }
            ?.let { (pos, _) -> listOf(listOf(chunk.copy(pos = pos))) }
            ?: emptyList()
    }
}
