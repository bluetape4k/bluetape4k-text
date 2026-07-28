package io.bluetape4k.tokenizer.korean.block

import io.bluetape4k.collections.sliding
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.trace
import io.bluetape4k.tokenizer.korean.tokenizer.KoreanChunker
import io.bluetape4k.tokenizer.korean.tokenizer.KoreanToken
import io.bluetape4k.tokenizer.korean.utils.KoreanPos


/**
 * 금칙어 필터를 우회하려고 단어 중간에 구두점을 끼워 넣는 패턴을 찾아 제거합니다.
 *
 * ## 동작/계약
 * - 길이 3 token sliding window를 사용한다. 가운데 token이 우회 문자([Punctuation], [KoreanPos.Email],
 *   [KoreanPos.Hashtag], [KoreanPos.CashTag])이고 양쪽 이웃 token이 일반 본문 token([normalPos])이면 가운데
 *   token을 제거 대상으로 표시한다.
 * - 원본 문자 offset을 보존하기 위해 제거는 뒤쪽 token부터 수행한다.
 * - 한국어 문자열 옆 URL이 조용히 삭제되지 않도록 [KoreanPos.URL] token은 우회 문자 집합에서 의도적으로 제외한다.
 *
 * ```kotlin
 * val cleaned = PunctuationProcessor().removePunctuation("섹.스")
 * // cleaned == "섹스"
 * ```
 */
class PunctuationProcessor {

    companion object: KLogging() {
        private val normalPos = arrayOf(
            KoreanPos.Korean,
            KoreanPos.KoreanParticle,
            KoreanPos.Foreign,
            KoreanPos.Number,
            KoreanPos.Alpha,
            KoreanPos.Adjective
        )
        private val punctuationPos = arrayOf(
            KoreanPos.Punctuation,
            KoreanPos.Email,
            KoreanPos.Hashtag,
            KoreanPos.CashTag,
        )
    }

    /**
     * 중간 구두점 제거 규칙에 따라 문자열을 정리합니다.
     *
     * ## 동작/계약
     * - `findPunctuation` 결과에서 제거 플래그가 `true`인 토큰 구간만 삭제한다.
     * - 삭제는 `tokens.reversed()` 순회로 수행한다.
     *
     * ```kotlin
     * val out = PunctuationProcessor().removePunctuation("찌~~~찌~뽕")
     * // out == "찌찌뽕"
     * ```
     *
     * @param text 구두점 우회 패턴을 제거할 입력 문자열입니다.
     * @return 제거 대상 구두점 token을 삭제한 문자열입니다.
     */
    fun removePunctuation(text: String): String {
        val tokens = findPunctuation(text)
        var result = text
        tokens.reversed()
            .forEach {
                val token = it.first
                log.trace { "remove punctuation token. offset=${token.offset}, length=${token.length}, remove=${it.second}" }
                if (it.second) {
                    result = result.removeRange(token.offset, token.offset + token.length)
                }
            }
        log.trace { "punctuation removed. beforeLength=${text.length}, afterLength=${result.length}" }
        return result
    }

    /**
     * 토큰 단위로 구두점 제거 가능 여부를 계산합니다.
     *
     * ## 동작/계약
     * - `KoreanChunker.chunk(text)` 결과를 길이 3 윈도우로 순회한다.
     * - 각 윈도우의 가운데 토큰에 대해 `canRemovePunctuation` 결과를 붙여 반환한다.
     *
     * ```kotlin
     * val pairs = PunctuationProcessor().findPunctuation("섹.스")
     * // pairs.any { it.first.text == "." && it.second } == true
     * ```
     *
     * @param text 구두점 제거 후보를 검사할 입력 문자열입니다.
     * @return 각 가운데 token과 제거 가능 여부를 묶은 list입니다.
     */
    fun findPunctuation(text: String): List<Pair<KoreanToken, Boolean>> {
        val chunks = KoreanChunker.chunk(text)

        return chunks
            // 공백 token까지 보존해야 원본 offset 기준 제거 위치가 유지된다.
            .sliding(3, false)
            .onEach { tokens -> log.trace { "sliding token window. size=${tokens.size}" } }
            .mapIndexed { index, tokens -> (index + 1) to canRemovePunctuation(tokens) }
            .map { chunks[it.first] to it.second }
            .onEach { log.trace { "punctuation candidate. offset=${it.first.offset}, length=${it.first.length}, remove=${it.second}" } }
    }


    private fun canRemovePunctuation(tokens: List<KoreanToken>): Boolean {
        if (tokens.size < 3) {
            return false
        }
        val prev = tokens[0]
        val current = tokens[1]
        val next = tokens[2]

        // 중간 token이 우회 구두점이고 앞뒤 token이 일반 본문 token이면 구두점을 제거할 수 있다고 판단한다.
        return current.pos in punctuationPos &&
                prev.pos in normalPos &&
                next.pos in normalPos
    }
}
