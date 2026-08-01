package io.bluetape4k.tokenizer.korean.tokenizer

import io.bluetape4k.collections.eclipse.multi.listMultimapOf
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.error
import io.bluetape4k.tokenizer.exceptions.TokenizerException
import io.bluetape4k.tokenizer.korean.stemmer.KoreanStemmer
import io.bluetape4k.tokenizer.korean.utils.KoreanDictionaryProvider.koreanDictionary
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Conjunction
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Korean
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Noun
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Unknown
import io.bluetape4k.tokenizer.korean.utils.KoreanPosTrie
import io.bluetape4k.tokenizer.korean.utils.KoreanPosx
import io.bluetape4k.tokenizer.korean.utils.KoreanSubstantive
import io.bluetape4k.tokenizer.model.requireTokenizeTextLength

/**
 * 명사 중심 규칙으로 한국어 문장을 분석하는 토크나이저입니다.
 *
 * ## 동작/계약
 * - 품사 규칙을 `Noun`과 `Conjunction` 위주로 제한해 일반 토크나이저보다 명사 분해를 우선한다.
 * - 비한글 청크는 그대로 유지하고, 한글 청크만 내부 파서를 적용한다.
 * - 최종 결과는 `KoreanStemmer.stem`을 통과하므로 용언이 포함된 경우 `stem` 정보가 채워질 수 있다.
 *
 * ```kotlin
 * val tokens = NounTokenizer.tokenize("성탄절 쇼핑")
 * // tokens.map { it.text } == ["성탄절", " ", "쇼핑"]
 * ```
 */
object NounTokenizer: KLogging() {

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
    //  private val SequenceDefinition = mapOf(
    //      // 체언
    //      "D0m*N1s0j0" to Noun,
    //      // 용언: 초기뻐하다, 와주세요, 초기뻤었고, 추첨하다, 구경하기힘들다, 기뻐하는, 기쁜, 추첨해서, 좋아하다, 걸려있을
    //      "v*V1r*e0" to Verb,
    //      "v*J1r*e0" to Adjective,
    //      // 수식언: 부사
    //      "A1" to Adverb,
    //      // 독립언
    //      "C1" to Conjunction,
    //      "E+" to Exclamation,
    //      "j1" to Josa)

    private val SequenceDefinition = mutableMapOf(
        // 체언
        "D0m*N1s0" to Noun,
        "C1" to Conjunction
    )

    /**
     * 명사 중심 품사 시퀀스 규칙을 트라이로 구성한 파서 상태 집합입니다.
     *
     * ## 동작/계약
     * - `SequenceDefinition`을 기반으로 lazy 초기화된다.
     * - 내부 동적 계획법 후보 확장에서 현재 상태 전이에 사용된다.
     *
     * ```kotlin
     * val trie = NounTokenizer.koreanPosTrie
     * // trie.isNotEmpty() == true
     * ```
     */
    internal val koreanPosTrie by lazy { KoreanPosx.getTrie(SequenceDefinition) }

    /**
     * 문장을 1-best 명사 중심 토큰 리스트로 분석합니다.
     *
     * ## 동작/계약
     * - `tokenizeTopN(text, 1, profile)`의 첫 후보를 사용한다.
     * - `KoreanTokenizerTest`와 `NounPhraseExtractorTest` 경로에서 명사구 추출 입력으로 사용된다.
     *
     * ```kotlin
     * val tokens = NounTokenizer.tokenize("떡 만두국")
     * // tokens.isNotEmpty() == true
     * ```
     */
    fun tokenize(
        text: CharSequence,
        profile: TokenizerProfile = TokenizerProfile.DefaultProfile,
    ): List<KoreanToken> {
        requireTokenizeTextLength(text)
        val tokenized = tokenizeTopN(text, 1, profile)
            .flatMap { it.firstOrNull() ?: emptyList() }

        return KoreanStemmer.stem(tokenized)
    }

    /**
     * 문장을 청크별 상위 `topN` 명사 중심 후보로 분석합니다.
     *
     * ## 동작/계약
     * - 반환 구조는 `List<청크, List<후보, List<KoreanToken>>>`이며 각 후보는 점수순으로 정렬된다.
     * - `topN`은 1 이상이어야 하며, 0 이하를 전달하면 `IllegalArgumentException`을 던진다.
     * - 후보가 비어 있으면 `unknown=true`인 `Noun` 단일 토큰 후보를 생성한다.
     * - 분석 중 예외는 원문 대신 입력 길이만 포함한 `TokenizerException`으로 변환된다.
     *
     * ```kotlin
     * val top = NounTokenizer.tokenizeTopN("허니버터칩", topN = 1)
     * // top.size >= 1
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
            return KoreanChunker.chunk(text)
                .map {
                    when (it.pos) {
                        Korean -> {
                            // 각 청크의 최적 분석 후보를 구합니다.
                            val parsed = parseKoreanChunk(it, profile, topN)

                            // 한 글자 명사가 이어진 구간을 하나의 unknown 명사로 접습니다: (가Noun 회Noun -> 가회Noun*)
                            parsed.map(KoreanSubstantive::collapseNouns)
                        }

                        else   -> listOf(listOf(it))
                    }
                }
        } catch (e: Exception) {
            log.error(e) { "Error tokenizing a chunk. textLength=${text.length}" }
            throw TokenizerException("Error tokenizing a chunk. textLength=${text.length}", e)
        }
    }

    /**
     * 동적 계획법으로 [chunk]의 상위 명사 중심 분석 후보를 구합니다.
     *
     * @param chunk 전체가 `Korean` 품사인 입력 청크입니다. 호출자가 청크 유효성을 보장하므로 성능을 위해 반복 검증하지 않습니다.
     * @param profile 토큰 점수 계산과 `spaceGuide` 적용 방식을 제어하는 프로필입니다.
     * @param topN 반환할 상위 후보 개수입니다.
     * @return 점수순으로 고른 상위 후보별 토큰 경로 목록입니다.
     */
    private fun parseKoreanChunk(
        chunk: KoreanToken,
        profile: TokenizerProfile = TokenizerProfile.DefaultProfile,
        topN: Int = 1,
    ): List<List<KoreanToken>> {
        return findTopCandidates(chunk, profile).take(topN)
    }

    private fun findTopCandidates(chunk: KoreanToken, profile: TokenizerProfile): List<List<KoreanToken>> {
        val directMatch: List<List<KoreanToken>> = findDirectMatch(chunk)
        val nounDictionary = koreanDictionary[Noun]

        // 위치별 후보 해석을 저장하는 버퍼입니다.
        val solutions = listMultimapOf<Int, CandidateParse>()
            .apply {
                val candidateParse = CandidateParse(
                    parse = ParsedChunk(listOf(), 1, profile),
                    curTrie = koreanPosTrie,
                    ending = null
                )
                put(0, candidateParse)
            }

        // 각 위치 상태마다 상위 N개 후보를 유지합니다.
        for (end in 1..chunk.length) {
            for (start in end - 1 downTo (end - MAX_TRACE_BACK).coerceAtLeast(0)) {
                val word = chunk.text.slice(start until end)
                val curSolutions = solutions[start]!!
                val candidates: List<CandidateParse> = curSolutions.flatMap { candateParse: CandidateParse ->

                    val possiblePoses: List<PossibleTrie> = candateParse.ending?.let {
                        candateParse.curTrie.map { PossibleTrie(it, 0) } + koreanPosTrie.map { PossibleTrie(it, 1) }
                    } ?: candateParse.curTrie.map { PossibleTrie(it, 0) }

                    possiblePoses
                        .filter {
                            it.curTrie.curPos == Noun ||
                                    (koreanDictionary[it.curTrie.curPos]?.contains(word) ?: false)
                        }
                        .map { t: PossibleTrie ->
                            val candidateToAdd =
                                if (t.curTrie.curPos == Noun &&
                                    nounDictionary?.contains(word) == false
                                ) {
                                    val isWordName: Boolean = KoreanSubstantive.isName(word)
                                    val isKoreanNumber = KoreanSubstantive.isKoreanNumber(word)
                                    val isWordKoreanNameVariation: Boolean =
                                        KoreanSubstantive.isKoreanNameVariation(word)

                                    val unknown = !isWordName && !isKoreanNumber && !isWordKoreanNameVariation
                                    val pos = Noun

                                    val token = KoreanToken(
                                        word,
                                        pos,
                                        chunk.offset + start,
                                        word.length,
                                        unknown = unknown
                                    )
                                    ParsedChunk(listOf(token), t.words, profile)

                                } else {
                                    val pos = t.curTrie.curPos ?: Unknown
                                    val token = KoreanToken(word, pos, chunk.offset + start, word.length)
                                    ParsedChunk(listOf(token), t.words, profile)
                                }

                            val nextTrie: List<KoreanPosTrie> =
                                t.curTrie.nextTrie
                                    ?.map { if (it == KoreanPosx.SelfNode) t.curTrie else it }
                                    ?: emptyList()

                            CandidateParse(candateParse.parse + candidateToAdd, nextTrie, t.curTrie.ending)
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

        val topCandidates = if (solutions[chunk.length]!!.isEmpty()) {
            listOf(listOf(KoreanToken(chunk.text, Noun, chunk.offset, chunk.length, unknown = true)))
        } else {
            solutions[chunk.length]!!
                .sortedBy { it.parse.score }
                .map { it.parse.posNodes }
        }

        return (directMatch + topCandidates).distinct()
    }

    private fun findDirectMatch(chunk: KoreanToken): List<List<KoreanToken>> {
        for ((pos, dict) in koreanDictionary.entries) {
            if (dict.contains(chunk.text)) {
                return listOf(listOf(chunk.copy(pos = pos)))
            }
        }
        return emptyList()
    }
}
