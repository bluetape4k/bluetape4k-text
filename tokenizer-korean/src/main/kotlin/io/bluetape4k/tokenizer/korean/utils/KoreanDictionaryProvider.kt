package io.bluetape4k.tokenizer.korean.utils

import io.bluetape4k.logging.KLogging
import io.bluetape4k.tokenizer.model.Severity
import io.bluetape4k.tokenizer.korean.utils.KoreanConjugation.conjugatePredicated
import io.bluetape4k.tokenizer.korean.utils.KoreanConjugation.conjugatePredicatesToCharArraySet
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Adjective
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Adverb
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Conjunction
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Determiner
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Eomi
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Exclamation
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Josa
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Modifier
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Noun
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.PreEomi
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Suffix
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Verb
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.VerbPrefix
import io.bluetape4k.tokenizer.utils.CharArraySet
import io.bluetape4k.tokenizer.utils.DictionarySnapshot
import io.bluetape4k.tokenizer.utils.DictionaryProvider
import io.bluetape4k.tokenizer.utils.DictionaryVersion
import io.bluetape4k.tokenizer.utils.SuspendMemoized
import io.bluetape4k.tokenizer.utils.VersionedDictionary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import java.util.Collections
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/** 금칙어 처리에서 함께 관찰해야 하는 한국어 세 사전의 immutable snapshot입니다. */
internal data class KoreanDictionaryBundleSnapshot(
    val dictionary: DictionarySnapshot<Map<KoreanPos, Set<String>>>,
    val blockwords: DictionarySnapshot<Map<Severity, Set<String>>>,
    val properNouns: DictionarySnapshot<Set<String>>,
)

/** cumulative 공개 view와 source severity tier를 함께 보존하는 immutable map입니다. */
private class CumulativeBlockwordMap(
    sourceTiers: Map<Severity, Set<String>>,
    cumulative: Map<Severity, Set<String>>,
): AbstractMap<Severity, Set<String>>(), java.io.Serializable {

    val sourceTiers: Map<Severity, Set<String>> = sourceTiers
    private val cumulative: Map<Severity, Set<String>> = cumulative

    override val entries: Set<Map.Entry<Severity, Set<String>>>
        get() = cumulative.entries

    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

/**
 * 토크나이저가 사용하는 한국어 사전과 파생 사전을 로드/조회합니다.
 *
 * ## 동작/계약
 * - 사전 데이터는 리소스 경로 `koreantext/` 하위에서 읽는다.
 * - 사전 loader는 suspend preload와 동기 facade가 공유하는 단일 lifecycle로 최초 접근 시점에 로딩된다.
 * - `addWordsToDictionary`로 런타임 단어를 추가하면 해당 품사 사전에 즉시 반영된다.
 *
 * ```kotlin
 * val nouns = KoreanDictionaryProvider.koreanDictionary.getValue(KoreanPos.Noun)
 * // nouns.isNotEmpty() == true
 * ```
 */
@Suppress("TooManyFunctions")
object KoreanDictionaryProvider: KLogging() {

    private val dictionaryMutationLock = ReentrantLock()

    // 사전 종류별 마지막 값 하나만 보관합니다. 기존 공개 뷰의 깊은 불변성은 유지합니다.
    private var dictionaryViewCache: Pair<Map<KoreanPos, Set<String>>, Map<KoreanPos, CharArraySet>>? = null
    private var blockwordViewCache: Pair<Map<Severity, Set<String>>, Map<Severity, CharArraySet>>? = null
    private var properNounViewCache: Pair<Set<String>, CharArraySet>? = null

    private val koreanDictionaryVersions = SuspendMemoized {
        VersionedDictionary(
            DictionarySnapshot(
                DictionaryVersion("korean-dictionary", 0),
                snapshotDictionaryValue(loadKoreanDictionary()),
            ),
            historyCapacity = 0,
        )
    }

    @PublishedApi
    internal val blockwordVersions = SuspendMemoized {
        val loaded = loadBlockWords()
        val exact = loaded.mapValues { (_, words) -> immutableSet(words.map { it.asDictionaryWord() }) }
        VersionedDictionary(
            DictionarySnapshot(
                DictionaryVersion("korean-blockwords", 0),
                canonicalBlockwordValue(exact),
            ),
            historyCapacity = 0,
        )
    }

    private val koreanEntityFreqLoader = SuspendMemoized {
        runInterruptible(Dispatchers.IO) {
            DictionaryProvider.readWordFreqs("$BASE_PATH/freq/entity-freq.txt.gz")
        }
    }

    private val spamNounsLoader = SuspendMemoized {
        readWords(
            "noun/spam.txt",
            "noun/profane.txt",
            "noun/slangs.txt",
        )
    }

    private val properNounsLoader = SuspendMemoized {
        readWords(
            "noun/entities.txt",
            "noun/names.txt",
            "noun/twitter.txt",
            "noun/lol.txt",
            "noun/company_names.txt",
            "noun/foreign.txt",
            "noun/geolocations.txt",
            "substantives/given_names.txt",
            "noun/kpop.txt",
            "noun/bible.txt",
            "noun/pokemon.txt",
            "noun/congress.txt",
            "noun/wikipedia_title_nouns.txt",
            "noun/brand.txt",
            "noun/fashion.txt",
            "noun/neologism.txt"
        )
    }

    private val properNounVersions = SuspendMemoized {
        VersionedDictionary(
            DictionarySnapshot(
                DictionaryVersion("korean-proper-nouns", 0),
                immutableSet(properNounsLoader.get().map { it.asDictionaryWord() }),
            ),
            historyCapacity = 0,
        )
    }

    private val nameDictionaryLoader = SuspendMemoized {
        coroutineScope {
            val familyName = async { readWords("substantives/family_names.txt") }
            val givenName = async { readWords("substantives/given_names.txt") }
            val fullName = async { readWords("noun/kpop.txt", "noun/foreign.txt", "noun/names.txt") }
            mapOf(
                "family_name" to familyName.await(),
                "given_name" to givenName.await(),
                "full_name" to fullName.await()
            )
        }
    }

    private val typoDictionaryByLengthLoader = SuspendMemoized {
        runInterruptible(Dispatchers.IO) {
            val grouped = DictionaryProvider.readWordMap("$BASE_PATH/typos/typos.txt")
                .groupBy { it.first.length }
            val result = mutableMapOf<Int, Map<String, String>>()

            grouped.forEach { (index, pair) ->
                result[index] = pair.associate { (k, v) -> k to v }
            }

            result
        }
    }

    private val predicateStemsLoader = SuspendMemoized {
        fun getConjugationMap(words: Set<String>, isAdjective: Boolean): Map<String, String> {
            return words
                .flatMap { word ->
                    conjugatePredicated(setOf(word), isAdjective).map {
                        it to word + "다"
                    }
                }
                .toMap()
        }

        coroutineScope {
            val verb = async { readWordsAsSet("verb/verb.txt") }
            val adjective = async { readWordsAsSet("adjective/adjective.txt") }
            mapOf(
                Verb to getConjugationMap(verb.await(), false),
                Adjective to getConjugationMap(adjective.await(), true)
            )
        }
    }

    /**
     * 한국어 사전 리소스의 루트 경로입니다.
     *
     * ## 동작/계약
     * - `readWords*` 계열 함수는 전달된 파일명 앞에 이 경로를 붙여 조회한다.
     *
     * ```kotlin
     * val base = KoreanDictionaryProvider.BASE_PATH
     * // base == "koreantext"
     * ```
     */
    const val BASE_PATH = "koreantext"

    /**
     * 리소스 파일들을 읽어 `MutableSet<String>`으로 반환합니다.
     *
     * ## 동작/계약
     * - 각 파일명은 `"$BASE_PATH/$filename"`로 변환해 `DictionaryProvider.readWordsAsSet`에 전달한다.
     * - 동일 단어 중복은 `MutableSet` 특성으로 제거된다.
     *
     * ```kotlin
     * val words = KoreanDictionaryProvider.readWordsAsSet("verb/verb.txt")
     * // words.isNotEmpty() == true
     * ```
     *
     * @param filenames `koreantext/` 기준의 상대 리소스 파일 경로 목록입니다.
     * @return 중복을 제거한 mutable 문자열 집합입니다.
     */
    suspend fun readWordsAsSet(vararg filenames: String): MutableSet<String> {
        return DictionaryProvider.readWordsAsSet(paths = filenames.map { "$BASE_PATH/$it" }.toTypedArray())
    }

    /**
     * 리소스 파일들을 읽어 `CharArraySet`으로 반환합니다.
     *
     * ## 동작/계약
     * - 각 파일명은 `"$BASE_PATH/$filename"`로 변환해 `DictionaryProvider.readWords`에 전달한다.
     * - 반환 타입은 토크나이저 사전 조회에 직접 사용되는 `CharArraySet`이다.
     *
     * ```kotlin
     * val words = KoreanDictionaryProvider.readWords("noun/nouns.txt")
     * // words.isNotEmpty() == true
     * ```
     *
     * @param filenames `koreantext/` 기준의 상대 리소스 파일 경로 목록입니다.
     * @return 토크나이저 사전 조회에 쓰는 `CharArraySet`입니다.
     */
    suspend fun readWords(vararg filenames: String): CharArraySet {
        return DictionaryProvider.readWords(paths = filenames.map { "$BASE_PATH/$it" }.toTypedArray())
    }

    /**
     * 한국어 토크나이저가 사용하는 모든 사전을 호출 코루틴을 차단하지 않고 미리 로드합니다.
     *
     * 동기 facade를 직접 처음 조회하면 기존 API 호환성을 위해 호출 스레드를 잠시 차단할 수
     * 있으므로 애플리케이션 시작 단계에서 이 함수를 호출하는 것을 권장합니다. 동시 호출은
     * 각 loader의 단일 초기화 결과를 공유하고, 취소/실패한 초기화는 다음 호출에서 재시도합니다.
     * 동기 facade가 초기화를 기다리는 중 interrupt되면 `InterruptedException`을 전달하고
     * 호출 스레드의 interrupt flag를 복구합니다.
     */
    suspend fun preload() {
        withContext(Dispatchers.IO) {
            listOf(
                async { koreanDictionaryVersions.get() },
                async { blockwordVersions.get() },
                async { koreanEntityFreqLoader.get() },
                async { spamNounsLoader.get() },
                async { properNounVersions.get() },
                async { nameDictionaryLoader.get() },
                async { typoDictionaryByLengthLoader.get() },
                async { predicateStemsLoader.get() },
            ).awaitAll()
        }
    }

    internal fun allDictionariesInitialized(): Boolean = listOf(
        koreanDictionaryVersions,
        blockwordVersions,
        koreanEntityFreqLoader,
        spamNounsLoader,
        properNounVersions,
        properNounsLoader,
        nameDictionaryLoader,
        typoDictionaryByLengthLoader,
        predicateStemsLoader,
    ).all(SuspendMemoized<*>::isInitialized)

    /**
     * 테스트마다 singleton loader lifecycle을 격리하기 위해 초기화 상태를 되돌립니다.
     *
     * 실제 애플리케이션 코드에서는 호출하지 않으며, public dictionary 계약에는 노출되지 않습니다.
     */
    internal suspend fun resetForTesting() {
        listOf(
            koreanDictionaryVersions,
            blockwordVersions,
            koreanEntityFreqLoader,
            spamNounsLoader,
            properNounVersions,
            properNounsLoader,
            nameDictionaryLoader,
            typoDictionaryByLengthLoader,
            predicateStemsLoader,
        ).forEach { it.clear() }
    }

    /**
     * 엔티티 빈도 사전입니다.
     *
     * ## 동작/계약
     * - `preload()` 또는 최초 동기 접근 시 `freq/entity-freq.txt.gz`를 로드한다.
     * - event-loop와 같은 호출 스레드 차단을 피하려면 애플리케이션 시작 단계에서 `preload()`를 호출한다.
     * - `ParsedChunk.getFreqScore()` 계산에 사용된다.
     * - `KoreanDictionaryProviderTest`의 `load frequency` 케이스에서 비어 있지 않음을 검증한다.
     *
     * ```kotlin
     * val freq = KoreanDictionaryProvider.koreanEntityFreq
     * // freq.isNotEmpty() == true
     * ```
     */
    val koreanEntityFreq: Map<CharSequence, Float>
        get() = koreanEntityFreqLoader.getBlocking()

    /**
     * 지정 품사 사전에 단어 컬렉션을 추가합니다.
     *
     * ## 동작/계약
     * - 대상 품사 사전이 존재할 때만 단어를 추가한다.
     * - 쓰기 경로는 내부 lock으로 직렬화해 동시성 환경에서 구조 손상을 방지한다.
     * - 사전이 없으면 아무 동작도 하지 않는다.
     * - `KoreanDictionaryProviderTest`에서 추가 후 포함 여부가 `true`로 바뀐다.
     *
     * ```kotlin
     * KoreanDictionaryProvider.addWordsToDictionary(KoreanPos.Noun, listOf("없는명사다"))
     * // KoreanDictionaryProvider.koreanDictionary.getValue(KoreanPos.Noun).contains("없는명사다") == true
     * ```
     *
     * @param pos 단어를 추가할 대상 품사입니다.
     * @param words 대상 사전에 추가할 단어 컬렉션입니다.
     */
    fun addWordsToDictionary(pos: KoreanPos, words: Collection<String>) {
        dictionaryMutationLock.withLock {
            val current = koreanDictionaryVersions.getBlocking().snapshot()
            val currentWords = current.value[pos]
            val changed = currentWords != null && words.any { it !in currentWords }
            val next = if (changed) {
                immutableMap(current.value + (pos to immutableSet(currentWords + words)))
            } else {
                current.value
            }
            publishDictionaryMutation(next)
        }
    }

    /**
     * 지정 품사 사전에 가변 인자 단어를 추가합니다.
     *
     * ## 동작/계약
     * - 인자가 비어 있지 않을 때만 추가를 시도한다.
     * - 쓰기 경로는 내부 lock으로 직렬화해 동시성 환경에서 구조 손상을 방지한다.
     * - 대상 품사 사전이 없으면 추가하지 않는다.
     *
     * ```kotlin
     * KoreanDictionaryProvider.addWordsToDictionary(KoreanPos.Noun, "주말특가", "주말행사")
     * // KoreanDictionaryProvider.koreanDictionary.getValue(KoreanPos.Noun).contains("주말특가") == true
     * ```
     *
     * @param pos 단어를 추가할 대상 품사입니다.
     * @param words 대상 사전에 추가할 가변 인자 단어 목록입니다.
     */
    fun addWordsToDictionary(pos: KoreanPos, vararg words: String) {
        if (words.isNotEmpty()) {
            addWordsToDictionary(pos, words.toList())
        }
    }

    /** 지정 품사 사전에서 단어 컬렉션을 제거하고 새 snapshot revision을 기록합니다. */
    fun removeWordsFromDictionary(pos: KoreanPos, words: Collection<String>) {
        dictionaryMutationLock.withLock {
            val current = koreanDictionaryVersions.getBlocking().snapshot()
            val currentWords = current.value[pos]
            val changed = currentWords != null && words.any { it in currentWords }
            val next = if (changed) {
                immutableMap(current.value + (pos to immutableSet(currentWords - words.toSet())))
            } else {
                current.value
            }
            publishDictionaryMutation(next)
        }
    }

    /** 현재 품사 사전 snapshot과 버전을 반환합니다. */
    fun currentDictionarySnapshot(): DictionarySnapshot<Map<KoreanPos, Set<String>>> =
        koreanDictionaryVersions.getBlocking().snapshot()

    /** 금칙어 처리 경로가 한 번에 읽는 세 사전의 immutable aggregate snapshot입니다. */
    internal fun currentBlockwordBundleSnapshot(): KoreanDictionaryBundleSnapshot =
        dictionaryMutationLock.withLock {
            KoreanDictionaryBundleSnapshot(
                dictionary = koreanDictionaryVersions.getBlocking().snapshot(),
                blockwords = blockwordVersions.getBlocking().snapshot(),
                properNouns = properNounVersions.getBlocking().snapshot(),
            )
        }

    /**
     * 품사별 사전을 새 버전으로 원자적으로 교체합니다.
     *
     * `dictionaries`는 전체 품사 사전 snapshot으로 취급합니다. loader 단계에서 값이 만들어진 뒤
     * 기존 map을 교체하므로 실패한 입력은 현재 사전을 변경하지 않습니다.
     *
     * @param version 현재 버전보다 큰 새 사전 버전입니다.
     * @param dictionaries 품사별 단어 목록입니다.
     * @return 공개된 사전 snapshot입니다.
     */
    fun reloadDictionaries(
        version: DictionaryVersion,
        dictionaries: Map<KoreanPos, Collection<String>>,
    ): DictionarySnapshot<Map<KoreanPos, Set<String>>> = dictionaryMutationLock.withLock {
        require(version.name == "korean-dictionary") { "Expected korean-dictionary version" }
        val replacement = dictionaries.mapValues { (_, words) -> words.toSet() }
        koreanDictionaryVersions.getBlocking().reload(version) { immutableMap(replacement) }
    }

    /**
     * 품사별 기본 한국어 사전입니다.
     *
     * ## 동작/계약
     * - `Noun`은 다수 noun 파일을 합쳐 로드한다.
     * - `Verb`/`Adjective`는 기본형 파일을 읽은 뒤 활용형 사전으로 확장한다.
     * - `KoreanDictionaryProviderTest`의 `사전 로드하기` 케이스에서 `Noun` 사전 비어 있지 않음을 검증한다.
     * - 반환값은 현재 immutable snapshot의 read-only 호환 view다.
     * - view에 직접 쓰기를 시도하면 `UnsupportedOperationException`이 발생하므로
     *   사전 변경에는 `addWordsToDictionary`/`removeWordsFromDictionary`를 사용한다.
     *
     * ```kotlin
     * val nouns = KoreanDictionaryProvider.koreanDictionary.getValue(KoreanPos.Noun)
     * // nouns.isNotEmpty() == true
     * ```
     */
    val koreanDictionary: Map<KoreanPos, CharArraySet>
        get() = publicDictionaryView(koreanDictionaryVersions.getBlocking().snapshot().value)

    private suspend fun loadKoreanDictionary(): Map<KoreanPos, CharArraySet> =
        withContext(Dispatchers.IO) {
            mutableMapOf<KoreanPos, CharArraySet>()
                .apply {
                    put(
                        Noun,
                        readWords(
                            "noun/nouns.txt",
                            "noun/entities.txt",
                            "noun/spam.txt",
                            "noun/names.txt",
                            "noun/twitter.txt",
                            "noun/lol.txt",
                            "noun/slangs.txt",
                            "noun/company_names.txt",
                            "noun/foreign.txt",
                            "noun/geolocations.txt",
                            "noun/profane.txt",
                            "substantives/given_names.txt",
                            "noun/kpop.txt",
                            "noun/bible.txt",
                            "noun/pokemon.txt",
                            "noun/congress.txt",
                            "noun/wikipedia_title_nouns.txt",
                            "noun/brand.txt",
                            "noun/fashion.txt",
                            "noun/commerce.txt",
                            "noun/neologism.txt",
                        )
                    )

                    val verbs = async { readWordsAsSet("verb/verb.txt") }
                    val adjective = async { readWordsAsSet("adjective/adjective.txt") }
                    val adveb = async { readWords("adverb/adverb.txt") }
                    val determiner = async { readWords("auxiliary/determiner.txt") }
                    val exclamation = async { readWords("auxiliary/exclamation.txt") }
                    val josa = async { readWords("josa/josa.txt") }
                    val eomi = async { readWords("verb/eomi.txt") }
                    val preEomi = async { readWords("verb/pre_eomi.txt") }
                    val conjuction = async { readWords("auxiliary/conjunctions.txt") }
                    val modifier = async { readWords("substantives/modifier.txt") }
                    val verbPrefix = async { readWords("verb/verb_prefix.txt") }
                    val suffix = async { readWords("substantives/suffix.txt") }

                    put(Verb, conjugatePredicatesToCharArraySet(verbs.await()))
                    put(Adjective, conjugatePredicatesToCharArraySet(adjective.await(), true))
                    put(Adverb, adveb.await())
                    put(Determiner, determiner.await())
                    put(Exclamation, exclamation.await())
                    put(Josa, josa.await())
                    put(Eomi, eomi.await())
                    put(PreEomi, preEomi.await())
                    put(Conjunction, conjuction.await())
                    put(Modifier, modifier.await())
                    put(VerbPrefix, verbPrefix.await())
                    put(Suffix, suffix.await())
                }
        }

    /**
     * 스팸/욕설/비속어 명사 사전입니다.
     *
     * ## 동작/계약
     * - `noun/spam.txt`, `noun/profane.txt`, `noun/slangs.txt`를 합쳐 로드한다.
     * - `KoreanPhraseExtractor`의 `filterSpam=true` 필터에서 사용된다.
     *
     * ```kotlin
     * val spam = KoreanDictionaryProvider.spamNouns
     * // spam.isNotEmpty() == true
     * ```
     */
    val spamNouns: CharArraySet
        get() = spamNounsLoader.getBlocking()

    /**
     * 심각도별 금칙어 사전입니다.
     *
     * ## 동작/계약
     * - `LOW`는 low/middle/high 파일 전체를 포함한다.
     * - `MIDDLE`은 middle/high를 포함하고, `HIGH`는 high만 포함한다.
     * - `KoreanBlockwordProcessor`에서 severity별 마스킹 판정에 사용된다.
     * - 반환값은 현재 immutable snapshot의 read-only 호환 view다.
     * - view에 직접 쓰기를 시도하면 `UnsupportedOperationException`이 발생하므로
     *   사전 변경에는 `KoreanProcessor.addBlockwords`/`KoreanProcessor.removeBlockwords`를 사용한다.
     *
     * ```kotlin
     * val high = KoreanDictionaryProvider.blockWords[io.bluetape4k.tokenizer.model.Severity.HIGH]
     * // high.isNotEmpty() == true
     * ```
     */
    val blockWords: Map<Severity, CharArraySet>
        get() = publicBlockwordView(blockwordVersions.getBlocking().snapshot().value)

    private suspend fun loadBlockWords(): Map<Severity, CharArraySet> =
        withContext(Dispatchers.IO) {
            val low = async { readWords("block/block_low.txt") }
            val middle = async { readWords("block/block_middle.txt") }
            val high = async { readWords("block/block_high.txt") }

            mapOf(
                Severity.LOW to low.await(),
                Severity.MIDDLE to middle.await(),
                Severity.HIGH to high.await(),
            )
        }

    /**
     * 현재 심각도별 cumulative 금칙어 snapshot과 버전을 반환합니다.
     * 반환 map을 그대로 `reloadBlockwords`에 전달하면 snapshot의 source tier provenance도 유지됩니다.
     */
    fun currentBlockwordSnapshot(): DictionarySnapshot<Map<Severity, Set<String>>> =
        blockwordVersions.getBlocking().snapshot()

    /**
     * 심각도별 금칙어 사전을 새 버전으로 교체합니다.
     *
     * @param version 현재 버전보다 큰 `korean-blockwords` 버전입니다.
     * @param wordsBySeverity 심각도별 금칙어 목록입니다. 일반 map은 exact source tier 입력으로
     *   해석합니다. `currentBlockwordSnapshot().value`를 복사하지 않고 그대로 전달한 map은
     *   snapshot이 보존한 source tier provenance을 사용하며, 공개 값은 항상 cumulative threshold
     *   view로 정규화됩니다.
     * @return 공개된 금칙어 snapshot입니다.
     */
    fun reloadBlockwords(
        version: DictionaryVersion,
        wordsBySeverity: Map<Severity, Collection<String>>,
    ): DictionarySnapshot<Map<Severity, Set<String>>> =
        dictionaryMutationLock.withLock {
            require(version.name == "korean-blockwords") { "Expected korean-blockwords version" }
            val current = blockwordVersions.getBlocking().snapshot()
            val replacement = canonicalBlockwordValue(exactBlockwordInput(wordsBySeverity), current.value)
            blockwordVersions.getBlocking().reload(version) { replacement }
        }

    /** 지정 심각도에서 금칙어가 존재하는지 확인합니다. */
    fun containsBlockword(text: String, severity: Severity): Boolean =
        blockwordVersions.getBlocking().snapshot().value[severity]?.contains(text) == true

    /** 기존 가변 금칙어 API가 갱신 버전도 기록하도록 내부 mutation을 감쌉니다. */
    internal inline fun mutateBlockwords(
        severity: Severity,
        action: CharArraySet.() -> Boolean,
    ) {
        dictionaryMutationLock.withLock {
            val current = blockwordVersions.getBlocking().snapshot()
            val exactTiers = currentExactBlockwordValue(current.value).toMutableMap()
            val words = CharArraySet(exactTiers.getValue(severity).toList())
            if (words.action()) {
                exactTiers[severity] = immutableSet(words.map { it.asDictionaryWord() })
                publishBlockwordMutation(canonicalBlockwordValue(exactTiers, current.value))
            } else {
                publishBlockwordMutation(current.value)
            }
        }
    }

    /** 금칙어·명사·고유명사를 하나의 revision bundle로 추가/삭제합니다. */
    internal fun mutateBlockwordBundle(
        words: Collection<String>,
        severity: Severity,
        add: Boolean,
    ) {
        dictionaryMutationLock.withLock {
            val dictionary = koreanDictionaryVersions.getBlocking().snapshot()
            val blockwords = blockwordVersions.getBlocking().snapshot()
            val properNouns = properNounVersions.getBlocking().snapshot()

            val exactTiers = currentExactBlockwordValue(blockwords.value).toMutableMap()
            val currentTier = exactTiers.getValue(severity)
            val nextTier = if (add) currentTier + words else currentTier - words.toSet()
            exactTiers[severity] = immutableSet(nextTier)
            val nextBlockwords = canonicalBlockwordValue(exactTiers, blockwords.value)

            val nextDictionary = dictionary.value[Noun]
                ?.takeIf { nouns -> words.any { if (add) it !in nouns else it in nouns } }
                ?.let { nouns ->
                    val nextNouns = if (add) nouns + words else nouns - words.toSet()
                    immutableMap(dictionary.value + (Noun to immutableSet(nextNouns)))
                }
                ?: dictionary.value

            val properChanged = words.any { if (add) it !in properNouns.value else it in properNouns.value }
            val nextProperNouns = if (properChanged) {
                immutableSet(if (add) properNouns.value + words else properNouns.value - words.toSet())
            } else {
                properNouns.value
            }

            val revision = maxOf(
                dictionary.version.revision,
                blockwords.version.revision,
                properNouns.version.revision,
            ) + 1
            publishDictionaryMutation(nextDictionary, revision)
            publishBlockwordMutation(nextBlockwords, revision)
            publishProperNounMutation(nextProperNouns, revision)
        }
    }

    /**
     * 고유명사 중심 명사 사전입니다.
     *
     * ## 동작/계약
     * - 인명/지명/브랜드 등 고유명사 성격 파일들을 합쳐 로드한다.
     * - phrase 추출 시 사전 포함 여부 판정에 활용된다.
     *
     * ```kotlin
     * val proper = KoreanDictionaryProvider.properNouns
     * // proper.isNotEmpty() == true
     * ```
     */
    val properNouns: CharArraySet
        get() = publicProperNounView(properNounVersions.getBlocking().snapshot().value)

    /**
     * 성/이름/전체 이름 분류 사전입니다.
     *
     * ## 동작/계약
     * - 키는 `"family_name"`, `"given_name"`, `"full_name"` 세 종류로 고정된다.
     * - `KoreanSubstantive.isName`이 이름 판별 시 이 맵을 조회한다.
     *
     * ```kotlin
     * val hasKim = KoreanDictionaryProvider.nameDictionary.getValue("family_name").contains("김")
     * // hasKim == true 또는 false
     * ```
     */
    val nameDictionary: Map<String, CharArraySet>
        get() = nameDictionaryLoader.getBlocking()

    /**
     * 오타 교정 사전을 원문 길이별로 그룹화한 맵입니다.
     *
     * ## 동작/계약
     * - `typos/typos.txt`를 읽어 오타 문자열 길이(`Int`) 기준으로 재구성한다.
     * - `KoreanNormalizer.correctTypo`에서 길이별 후보 조회에 사용된다.
     *
     * ```kotlin
     * val grouped = KoreanDictionaryProvider.typoDictionaryByLength
     * // grouped.keys.isNotEmpty() == true
     * ```
     */
    val typoDictionaryByLength: Map<Int, Map<String, String>>
        get() = typoDictionaryByLengthLoader.getBlocking()

    /**
     * 활용형 표면형을 기본형으로 역매핑한 사전입니다.
     *
     * ## 동작/계약
     * - 동사/형용사 기본형 파일을 읽고 활용형을 생성해 `표면형 -> 기본형다` 맵으로 구성한다.
     * - `KoreanStemmer.stem`에서 용언 토큰의 `stem` 계산에 사용된다.
     *
     * ```kotlin
     * val stem = KoreanDictionaryProvider.predicateStems[KoreanPos.Verb]?.get("해")
     * // stem == "하다"
     * ```
     */
    val predicateStems: Map<KoreanPos, Map<String, String>>
        get() = predicateStemsLoader.getBlocking()

    private fun snapshotDictionaryValue(dictionary: Map<KoreanPos, CharArraySet>): Map<KoreanPos, Set<String>> =
        immutableMap(dictionary.mapValues { (_, words) -> immutableSet(words.map { it.asDictionaryWord() }) })

    private fun publicDictionaryView(value: Map<KoreanPos, Set<String>>): Map<KoreanPos, CharArraySet> =
        dictionaryMutationLock.withLock {
            dictionaryViewCache?.takeIf { it.first === value }?.second ?: Collections.unmodifiableMap(
                value.mapValues { (_, words) ->
                    CharArraySet.unmodifiableSet(CharArraySet(words.toList()))
                }
            ).also { dictionaryViewCache = value to it }
        }

    private fun publicBlockwordView(value: Map<Severity, Set<String>>): Map<Severity, CharArraySet> =
        dictionaryMutationLock.withLock {
            blockwordViewCache?.takeIf { it.first === value }?.second ?: Collections.unmodifiableMap(
                value.mapValues { (_, words) ->
                    CharArraySet.unmodifiableSet(CharArraySet(words.toList()))
                }
            ).also { blockwordViewCache = value to it }
        }

    private fun publicProperNounView(value: Set<String>): CharArraySet =
        dictionaryMutationLock.withLock {
            properNounViewCache?.takeIf { it.first === value }?.second
                ?: CharArraySet.unmodifiableSet(CharArraySet(value.toList()))
                    .also { properNounViewCache = value to it }
        }

    private fun Any.asDictionaryWord(): String = when (this) {
        is CharArray -> concatToString()
        else -> toString()
    }

    private fun publishDictionaryMutation(value: Map<KoreanPos, Set<String>>, revision: Long? = null) {
        val current = koreanDictionaryVersions.getBlocking().snapshot()
        koreanDictionaryVersions.getBlocking().reload(
            DictionaryVersion(current.version.name, revision ?: current.version.revision + 1)
        ) { value }
    }

    private fun publishBlockwordMutation(value: Map<Severity, Set<String>>, revision: Long? = null) {
        val current = blockwordVersions.getBlocking().snapshot()
        blockwordVersions.getBlocking().reload(
            DictionaryVersion(current.version.name, revision ?: current.version.revision + 1)
        ) { value }
    }

    private fun publishProperNounMutation(value: Set<String>, revision: Long? = null) {
        val current = properNounVersions.getBlocking().snapshot()
        properNounVersions.getBlocking().reload(
            DictionaryVersion(current.version.name, revision ?: current.version.revision + 1)
        ) { value }
    }

    private fun <K, V> immutableMap(value: Map<K, V>): Map<K, V> = Collections.unmodifiableMap(value.toMap())

    private fun immutableSet(value: Collection<String>): Set<String> = Collections.unmodifiableSet(value.toSet())

    /**
     * exact-tier 입력을 threshold 조회용 cumulative view로 정규화합니다.
     *
     * `LOW` 조회에는 모든 tier, `MIDDLE` 조회에는 middle/high, `HIGH` 조회에는 high만 포함합니다.
     * 따라서 기존 cumulative view를 다시 입력해도 같은 결과가 유지됩니다.
     */
    private fun canonicalBlockwordValue(
        wordsBySeverity: Map<Severity, Collection<String>>,
        current: Map<Severity, Set<String>>? = null,
    ): Map<Severity, Set<String>> {
        val exactTiers = Severity.values().associateWith { severity ->
            immutableSet(wordsBySeverity[severity].orEmpty())
        }
        val desired = mapOf(
            Severity.LOW to exactTiers.getValue(Severity.LOW) +
                    exactTiers.getValue(Severity.MIDDLE) + exactTiers.getValue(Severity.HIGH),
            Severity.MIDDLE to exactTiers.getValue(Severity.MIDDLE) + exactTiers.getValue(Severity.HIGH),
            Severity.HIGH to exactTiers.getValue(Severity.HIGH),
        )
        val next = desired.mapValues { (severity, words) ->
            current?.get(severity)?.takeIf { it == words } ?: immutableSet(words)
        }
        val currentWithProvenance = current as? CumulativeBlockwordMap
        return if (currentWithProvenance != null &&
            currentWithProvenance.sourceTiers == exactTiers &&
            Severity.values().all { current[it] === next[it] }
        ) {
            current
        } else {
            CumulativeBlockwordMap(immutableMap(exactTiers), immutableMap(next))
        }
    }

    /** map snapshot은 source tier를 보존하고, 일반 map은 새 exact-tier 입력으로 해석합니다. */
    private fun exactBlockwordInput(
        value: Map<Severity, Collection<String>>,
    ): Map<Severity, Collection<String>> =
        (value as? CumulativeBlockwordMap)?.sourceTiers ?: value

    /** 현재 snapshot에 보존된 source tier를 반환합니다. */
    private fun currentExactBlockwordValue(value: Map<Severity, Set<String>>): Map<Severity, Set<String>> =
        (value as? CumulativeBlockwordMap)?.sourceTiers
            ?: error("Blockword snapshot does not carry source tier provenance")
}
