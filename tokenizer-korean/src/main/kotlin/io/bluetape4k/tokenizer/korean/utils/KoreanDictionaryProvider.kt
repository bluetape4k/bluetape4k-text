package io.bluetape4k.tokenizer.korean.utils

import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.publicLazy
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
import io.bluetape4k.tokenizer.utils.VersionedDictionary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * 토크나이저가 사용하는 한국어 사전과 파생 사전을 로드/조회합니다.
 *
 * ## 동작/계약
 * - 사전 데이터는 리소스 경로 `koreantext/` 하위에서 읽는다.
 * - 대부분 프로퍼티는 `lazy` 또는 `publicLazy`로 최초 접근 시점에 로딩된다.
 * - `addWordsToDictionary`로 런타임 단어를 추가하면 해당 품사 사전에 즉시 반영된다.
 *
 * ```kotlin
 * val nouns = KoreanDictionaryProvider.koreanDictionary[KoreanPos.Noun]
 * // nouns != null
 * ```
 */
@Suppress("TooManyFunctions")
object KoreanDictionaryProvider: KLogging() {

    private val dictionaryMutationLock = ReentrantLock()

    private val koreanDictionaryVersions by lazy {
        VersionedDictionary(
            DictionarySnapshot(
                DictionaryVersion("korean-dictionary", 0),
                snapshotDictionaryValue(),
            ),
            historyCapacity = 0,
        )
    }

    private val blockwordVersions by lazy {
        VersionedDictionary(
            DictionarySnapshot(
                DictionaryVersion("korean-blockwords", 0),
                snapshotBlockwordValue(),
            ),
            historyCapacity = 0,
        )
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
     * 엔티티 빈도 사전입니다.
     *
     * ## 동작/계약
     * - 최초 접근 시 `freq/entity-freq.txt.gz`를 로드한다.
     * - `ParsedChunk.getFreqScore()` 계산에 사용된다.
     * - `KoreanDictionaryProviderTest`의 `load frequency` 케이스에서 비어 있지 않음을 검증한다.
     *
     * ```kotlin
     * val freq = KoreanDictionaryProvider.koreanEntityFreq
     * // freq.isNotEmpty() == true
     * ```
     */
    val koreanEntityFreq: Map<CharSequence, Float> by lazy {
        runBlocking(Dispatchers.IO) {
            DictionaryProvider.readWordFreqs("$BASE_PATH/freq/entity-freq.txt.gz")
        }
    }

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
     * // KoreanDictionaryProvider.koreanDictionary[KoreanPos.Noun]!!.contains("없는명사다") == true
     * ```
     *
     * @param pos 단어를 추가할 대상 품사입니다.
     * @param words 대상 사전에 추가할 단어 컬렉션입니다.
     */
    fun addWordsToDictionary(pos: KoreanPos, words: Collection<String>) {
        dictionaryMutationLock.withLock {
            val changed = koreanDictionary[pos]?.addAll(words) == true
            publishDictionaryMutation(pos.takeIf { changed })
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
     * // KoreanDictionaryProvider.koreanDictionary[KoreanPos.Noun]!!.contains("주말특가") == true
     * ```
     *
     * @param pos 단어를 추가할 대상 품사입니다.
     * @param words 대상 사전에 추가할 가변 인자 단어 목록입니다.
     */
    fun addWordsToDictionary(pos: KoreanPos, vararg words: String) {
        if (words.isNotEmpty()) {
            dictionaryMutationLock.withLock {
                val changed = koreanDictionary[pos]?.addAll(words) == true
                publishDictionaryMutation(pos.takeIf { changed })
            }
        }
    }

    /** 지정 품사 사전에서 단어 컬렉션을 제거하고 새 snapshot revision을 기록합니다. */
    fun removeWordsFromDictionary(pos: KoreanPos, words: Collection<String>) {
        dictionaryMutationLock.withLock {
            val changed = koreanDictionary[pos]?.removeAll(words) == true
            publishDictionaryMutation(pos.takeIf { changed })
        }
    }

    /** 현재 품사 사전 snapshot과 버전을 반환합니다. */
    fun currentDictionarySnapshot(): DictionarySnapshot<Map<KoreanPos, Set<String>>> =
        koreanDictionaryVersions.snapshot()

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
        val snapshot = koreanDictionaryVersions.reload(version) { replacement }
        koreanDictionary.clear()
        replacement.forEach { (pos, words) -> koreanDictionary[pos] = CharArraySet(words.toList()) }
        snapshot
    }

    /**
     * 품사별 기본 한국어 사전입니다.
     *
     * ## 동작/계약
     * - `Noun`은 다수 noun 파일을 합쳐 로드한다.
     * - `Verb`/`Adjective`는 기본형 파일을 읽은 뒤 활용형 사전으로 확장한다.
     * - `KoreanDictionaryProviderTest`의 `사전 로드하기` 케이스에서 `Noun` 사전 비어 있지 않음을 검증한다.
     * - 반환된 mutable collection에 직접 쓰는 변경은 versioned snapshot에 기록되지 않으므로
     *   snapshot revision이 필요하면 `addWordsToDictionary`/`removeWordsFromDictionary`를 사용한다.
     *
     * ```kotlin
     * val nouns = KoreanDictionaryProvider.koreanDictionary[KoreanPos.Noun]
     * // nouns!!.isNotEmpty() == true
     * ```
     */
    val koreanDictionary: MutableMap<KoreanPos, CharArraySet> by lazy {
        runBlocking(Dispatchers.IO) {
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
    val spamNouns by lazy {
        runBlocking(Dispatchers.IO) {
            readWords(
                "noun/spam.txt",
                "noun/profane.txt",
                "noun/slangs.txt",
            )
        }
    }

    /**
     * 심각도별 금칙어 사전입니다.
     *
     * ## 동작/계약
     * - `LOW`는 low/middle/high 파일 전체를 포함한다.
     * - `MIDDLE`은 middle/high를 포함하고, `HIGH`는 high만 포함한다.
     * - `KoreanBlockwordProcessor`에서 severity별 마스킹 판정에 사용된다.
     * - 반환된 mutable collection에 직접 쓰는 변경은 versioned snapshot에 기록되지 않는다.
     *
     * ```kotlin
     * val high = KoreanDictionaryProvider.blockWords[io.bluetape4k.tokenizer.model.Severity.HIGH]
     * // high != null
     * ```
     */
    val blockWords by publicLazy {
        runBlocking(Dispatchers.IO) {
            val low = async { readWords("block/block_low.txt", "block/block_middle.txt", "block/block_high.txt") }
            val middle = async { readWords("block/block_middle.txt", "block/block_high.txt") }
            val high = async { readWords("block/block_high.txt") }

            mapOf(
                Severity.LOW to low.await(),
                Severity.MIDDLE to middle.await(),
                Severity.HIGH to high.await(),
            )
        }
    }

    /** 현재 심각도별 금칙어 snapshot과 버전을 반환합니다. */
    fun currentBlockwordSnapshot(): DictionarySnapshot<Map<Severity, Set<String>>> =
        blockwordVersions.snapshot()

    /**
     * 심각도별 금칙어 사전을 새 버전으로 교체합니다.
     *
     * @param version 현재 버전보다 큰 `korean-blockwords` 버전입니다.
     * @param wordsBySeverity 심각도별 전체 금칙어 목록입니다.
     * @return 공개된 금칙어 snapshot입니다.
     */
    fun reloadBlockwords(
        version: DictionaryVersion,
        wordsBySeverity: Map<Severity, Collection<String>>,
    ): DictionarySnapshot<Map<Severity, Set<String>>> =
        dictionaryMutationLock.withLock {
            require(version.name == "korean-blockwords") { "Expected korean-blockwords version" }
            val replacement = wordsBySeverity.mapValues { (_, words) -> words.toSet() }
            val snapshot = blockwordVersions.reload(version) { replacement }
            blockWords.forEach { (severity, words) ->
                words.clear()
                words.addAll(replacement[severity].orEmpty())
            }
            snapshot
        }

    /** 지정 심각도에서 금칙어가 존재하는지 확인합니다. */
    fun containsBlockword(text: String, severity: Severity): Boolean =
        dictionaryMutationLock.withLock { blockWords[severity]?.contains(text) == true }

    /** 기존 가변 금칙어 API가 갱신 버전도 기록하도록 내부 mutation을 감쌉니다. */
    internal inline fun mutateBlockwords(
        severity: Severity,
        action: CharArraySet.() -> Boolean,
    ) {
        dictionaryMutationLock.withLock {
            val affectedSeverities = affectedSeverities(severity)
            var changed = false
            affectedSeverities.forEach { affectedSeverity ->
                if (blockWords[affectedSeverity]?.action() == true) {
                    changed = true
                }
            }
            publishBlockwordMutation(affectedSeverities.takeIf { changed })
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
    val properNouns by publicLazy {
        runBlocking(Dispatchers.IO) {
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
    }

    /**
     * 성/이름/전체 이름 분류 사전입니다.
     *
     * ## 동작/계약
     * - 키는 `"family_name"`, `"given_name"`, `"full_name"` 세 종류로 고정된다.
     * - `KoreanSubstantive.isName`이 이름 판별 시 이 맵을 조회한다.
     *
     * ```kotlin
     * val hasKim = KoreanDictionaryProvider.nameDictionary["family_name"]!!.contains("김")
     * // hasKim == true 또는 false
     * ```
     */
    val nameDictionary: Map<String, CharArraySet> by publicLazy {
        runBlocking(Dispatchers.IO) {
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
    val typoDictionaryByLength: Map<Int, Map<String, String>> by publicLazy {
        runBlocking(Dispatchers.IO) {
            val grouped = DictionaryProvider.readWordMap("$BASE_PATH/typos/typos.txt")
                .groupBy { it.first.length }
            // val grouped = readWordMap("typos/typos.txt").toList().groupBy { it.first.length }
            val result = mutableMapOf<Int, Map<String, String>>()

            grouped.forEach { (index, pair) ->
                result[index] = pair.associate { (k, v) -> k to v }
            }

            result
        }
    }

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
    val predicateStems: Map<KoreanPos, Map<String, String>> by publicLazy {
        fun getConjugationMap(words: Set<String>, isAdjective: Boolean): Map<String, String> {
            return words
                .flatMap { word ->
                    conjugatePredicated(setOf(word), isAdjective).map {
                        it to word + "다"
                    }
                }
                .toMap()
        }

        runBlocking(Dispatchers.IO) {
            val verb = async { readWordsAsSet("verb/verb.txt") }
            val adjective = async { readWordsAsSet("adjective/adjective.txt") }
            mapOf(
                Verb to getConjugationMap(verb.await(), false),
                Adjective to getConjugationMap(adjective.await(), true)
            )
        }
    }

    private fun snapshotDictionaryValue(): Map<KoreanPos, Set<String>> =
        koreanDictionary.mapValues { (_, words) -> words.map { it.asDictionaryWord() }.toSet() }

    private fun snapshotDictionaryValue(
        currentValue: Map<KoreanPos, Set<String>>,
        changedPos: KoreanPos?,
    ): Map<KoreanPos, Set<String>> {
        val pos = changedPos
        val words = pos?.let { koreanDictionary[it] }
        return if (pos == null || words == null) {
            currentValue
        } else {
            currentValue + (pos to words.map { it.asDictionaryWord() }.toSet())
        }
    }

    private fun snapshotBlockwordValue(): Map<Severity, Set<String>> =
        blockWords.mapValues { (_, words) -> words.map { it.asDictionaryWord() }.toSet() }

    private fun snapshotBlockwordValue(
        currentValue: Map<Severity, Set<String>>,
        changedSeverities: Set<Severity>?,
    ): Map<Severity, Set<String>> {
        if (changedSeverities == null) {
            return currentValue
        }
        return currentValue.mapValues { (severity, previous) ->
            if (severity in changedSeverities) {
                blockWords[severity]?.map { it.asDictionaryWord() }?.toSet() ?: previous
            } else {
                previous
            }
        }
    }

    private fun Any.asDictionaryWord(): String = when (this) {
        is CharArray -> concatToString()
        else -> toString()
    }

    private fun publishDictionaryMutation(changedPos: KoreanPos? = null) {
        val current = koreanDictionaryVersions.snapshot()
        koreanDictionaryVersions.reload(
            DictionaryVersion(current.version.name, current.version.revision + 1)
        ) { snapshotDictionaryValue(current.value, changedPos) }
    }

    private fun publishBlockwordMutation(changedSeverities: Set<Severity>? = null) {
        val current = blockwordVersions.snapshot()
        blockwordVersions.reload(
            DictionaryVersion(current.version.name, current.version.revision + 1)
        ) { snapshotBlockwordValue(current.value, changedSeverities) }
    }

    private fun affectedSeverities(severity: Severity): Set<Severity> = when (severity) {
        Severity.LOW -> setOf(Severity.LOW, Severity.MIDDLE, Severity.HIGH)
        Severity.MIDDLE -> setOf(Severity.MIDDLE, Severity.HIGH)
        Severity.HIGH -> setOf(Severity.HIGH)
    }
}
