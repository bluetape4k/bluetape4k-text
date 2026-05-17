package io.bluetape4k.tokenizer.korean.block

import io.bluetape4k.collections.sliding
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.trace
import io.bluetape4k.tokenizer.korean.tokenizer.KoreanChunker
import io.bluetape4k.tokenizer.korean.tokenizer.KoreanToken
import io.bluetape4k.tokenizer.korean.utils.KoreanPos


/**
 * Identifies and removes mid-word evasion patterns that insert punctuation to bypass blockword filters.
 *
 * ## Behavior / Contract
 * - Uses a sliding window of 3 tokens: removes the middle token when it is classified as an
 *   evasion character ([Punctuation], [KoreanPos.Email], [KoreanPos.Hashtag], or [KoreanPos.CashTag])
 *   and both neighbours are normal content tokens ([normalPos]).
 * - Removal proceeds in reverse order to preserve original character offsets.
 * - [KoreanPos.URL] tokens are intentionally excluded from the evasion set so that URLs
 *   adjacent to Korean text are not silently deleted.
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
     */
    fun removePunctuation(text: String): String {
        val tokens = findPunctuation(text)
        var result = text
        tokens.reversed()
            .forEach {
                val token = it.first
                log.trace { "remove token. $it" }
                if (it.second) {
                    result = result.removeRange(token.offset, token.offset + token.length)
                }
            }
        log.trace { "chunk removed text=$result" }
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
     */
    fun findPunctuation(text: String): List<Pair<KoreanToken, Boolean>> {
        val chunks = KoreanChunker.chunk(text)

        return chunks
            // .filter { it.pos != KoreanPos.Space }
            .sliding(3, false)
            .onEach { log.trace { "sliding tokens=$it" } }
            .mapIndexed { index, tokens -> (index + 1) to canRemovePunctuation(tokens) }
            .map { chunks[it.first] to it.second }
            .onEach { log.trace { "can remove punctuation=$it" } }
    }


    private fun canRemovePunctuation(tokens: List<KoreanToken>): Boolean {
        if (tokens.size < 3) {
            return false
        }
        val prev = tokens[0]
        val current = tokens[1]
        val next = tokens[2]

        // 중간에 있는 token이 Punctuation이고, 앞뒤로 있는 token이 일반 token이면 Puctuation을 제거할 수 있다고 판단합니다.
        return current.pos in punctuationPos &&
                prev.pos in normalPos &&
                next.pos in normalPos
    }
}
