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
 * - 일반 본문 token([normalPos]) 사이의 우회 구두점([Punctuation], [KoreanPos.Email],
 *   [KoreanPos.Hashtag], [KoreanPos.CashTag])을 제거 대상으로 표시한다.
 * - 공백을 사이에 둔 run은 우회 표식이 있는 구두점이 둘 이상일 때만 해당 구두점과 공백을 제거한다.
 *   `?`, `!`, `.` 같은 문장 구분자만 있는 정상 구두점 run은 보존한다.
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
        private val sentencePunctuation = setOf('.', '?', '!', ',', ';', ':', '。', '？', '！', '，', '；', '：', '…')
    }

    /**
     * 중간 구두점 제거 규칙에 따라 문자열을 정리합니다.
     *
     * ## 동작/계약
     * - 우회 구두점 구간과 필요한 경우 그 사이 공백의 제거 플래그를 계산한다.
     * - 삭제는 `tokens.reversed()` 순회로 수행한다.
     *
     * ```kotlin
     * val out = PunctuationProcessor().removePunctuation("찌~~~찌~뽕")
     * // out == "찌찌뽕"
     * ```
     *
     * @param text 구두점 우회 패턴을 제거할 입력 문자열입니다.
     * @return 제거 대상 구두점과 필요한 공백을 삭제한 문자열입니다.
     */
    fun removePunctuation(text: String): String {
        val tokens = findPunctuation(text)
        var result = text
        tokens.reversed().forEach { (token, shouldRemove) ->
            log.trace {
                "remove punctuation token. offset=${token.offset}, " +
                        "length=${token.length}, remove=$shouldRemove"
            }
            if (shouldRemove) {
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
     * - `KoreanChunker.chunk(text)` 결과에서 공백을 건너뛴 앞뒤 일반 token을 기준으로
     *   우회 구두점 구간을 찾는다.
     * - 공백을 포함한 구간은 우회 표식이 있는 구두점이 둘 이상일 때만 구두점과 공백을 제거 대상으로 표시한다.
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
        val removable = findRemovableTokens(chunks)

        return chunks
            // 공백 token까지 보존해야 원본 offset 기준 제거 위치가 유지된다.
            .sliding(3, false)
            .onEach { tokens -> log.trace { "sliding token window. size=${tokens.size}" } }
            .mapIndexed { index, _ ->
                val token = chunks[index + 1]
                token to removable[index + 1]
            }
            .onEach { log.trace { "punctuation candidate. offset=${it.first.offset}, length=${it.first.length}, remove=${it.second}" } }
    }


    private fun findRemovableTokens(chunks: List<KoreanToken>): BooleanArray {
        val removable = BooleanArray(chunks.size)
        var index = 0

        while (index < chunks.size) {
            val nextIndex = if (isPunctuationOrSpace(chunks[index])) {
                val runEnd = findRunEnd(chunks, index)
                markRemovableRun(chunks, index, runEnd, removable)
                runEnd
            } else {
                index + 1
            }
            index = nextIndex
        }

        return removable
    }

    private fun findRunEnd(chunks: List<KoreanToken>, start: Int): Int {
        var end = start
        while (end < chunks.size && isPunctuationOrSpace(chunks[end])) {
            end++
        }
        return end
    }

    private fun markRemovableRun(
        chunks: List<KoreanToken>,
        runStart: Int,
        runEnd: Int,
        removable: BooleanArray,
    ) {
        val previousIndex = runStart - 1
        if (previousIndex < 0 || runEnd >= chunks.size) {
            return
        }
        if (chunks[previousIndex].pos !in normalPos || chunks[runEnd].pos !in normalPos) {
            return
        }

        val run = runStart until runEnd
        val punctuationCount = run.count { chunks[it].pos in punctuationPos }
        val hasSpace = run.any { chunks[it].pos == KoreanPos.Space }
        if (shouldRemoveRun(chunks, run, punctuationCount, hasSpace)) {
            run.filter { chunks[it].pos in punctuationPos }
                .forEach { removable[it] = true }
            if (punctuationCount > 1) {
                run.filter { chunks[it].pos == KoreanPos.Space }
                    .forEach { removable[it] = true }
            }
        }
    }

    private fun shouldRemoveRun(
        chunks: List<KoreanToken>,
        run: IntRange,
        punctuationCount: Int,
        hasSpace: Boolean,
    ): Boolean {
        return !hasSpace ||
                (punctuationCount > 1 && run.any { index ->
                    val token = chunks[index]
                    token.pos in punctuationPos && token.text.any { it !in sentencePunctuation }
                })
    }

    private fun isPunctuationOrSpace(token: KoreanToken): Boolean =
        token.pos in punctuationPos || token.pos == KoreanPos.Space
}
