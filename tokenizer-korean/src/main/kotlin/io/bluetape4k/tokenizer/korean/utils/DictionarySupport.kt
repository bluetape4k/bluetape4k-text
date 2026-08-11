package io.bluetape4k.tokenizer.korean.utils

/**
 * 이름 사전의 특정 분류에 [charseq]가 포함되는지 확인합니다.
 *
 * ## 동작/계약
 * - `KoreanDictionaryProvider.nameDictionary[key]`가 없으면 `false`를 반환한다.
 * - 사전이 있으면 [cs]를 문자열로 변환해 immutable snapshot과 비교한다.
 *
 * ```kotlin
 * val exists = nameDictionaryContains("family_name", "김")
 * // exists == true 또는 false
 * ```
 *
 * @param key 이름 사전의 분류 키입니다.
 * @param charseq 조회할 이름 후보 문자 시퀀스입니다.
 * @return 해당 분류 사전에 후보가 포함되면 `true`입니다.
 */
fun nameDictionaryContains(key: String, charseq: CharSequence): Boolean =
    KoreanDictionaryProvider.nameDictionary[key]?.contains(charseq) ?: false

/**
 * 이름 사전의 특정 분류에 [str]이 포함되는지 확인합니다.
 *
 * ## 동작/계약
 * - `KoreanDictionaryProvider.nameDictionary[key]`가 없으면 `false`를 반환한다.
 * - 사전이 있으면 `CharArraySet.contains(String)` 결과를 그대로 반환한다.
 *
 * ```kotlin
 * val exists = nameDictionaryContains("full_name", "문재인")
 * // exists == true 또는 false
 * ```
 *
 * @param key 이름 사전의 분류 키입니다.
 * @param str 조회할 이름 후보 문자열입니다.
 * @return 해당 분류 사전에 후보가 포함되면 `true`입니다.
 */
fun nameDictionaryContains(key: String, str: String): Boolean =
    KoreanDictionaryProvider.nameDictionary[key]?.contains(str) ?: false

/**
 * 품사별 사전에 [cs]가 포함되는지 확인합니다.
 *
 * ## 동작/계약
 * - `KoreanDictionaryProvider.koreanDictionary[pos]`가 없으면 `false`를 반환한다.
 * - 사전이 있으면 `CharArraySet.contains(CharSequence)` 결과를 그대로 반환한다.
 *
 * ```kotlin
 * val exists = koreanContains(KoreanPos.Noun, "사랑")
 * // exists == true 또는 false
 * ```
 *
 * @param pos 조회할 품사 사전입니다.
 * @param cs 조회할 후보 문자 시퀀스입니다.
 * @return 해당 품사 사전에 후보가 포함되면 `true`입니다.
 */
fun koreanContains(pos: KoreanPos, cs: CharSequence): Boolean =
    KoreanDictionaryProvider.currentDictionarySnapshot().value[pos]?.contains(cs.toString()) == true
