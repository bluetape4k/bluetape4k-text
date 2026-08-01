package io.bluetape4k.tokenizer.korean

import io.bluetape4k.tokenizer.korean.phrase.KoreanPhrase
import io.bluetape4k.tokenizer.korean.tokenizer.KoreanChunk
import io.bluetape4k.tokenizer.korean.tokenizer.KoreanToken
import io.bluetape4k.tokenizer.korean.tokenizer.Sentence
import io.bluetape4k.tokenizer.korean.tokenizer.TokenizerProfile
import io.bluetape4k.tokenizer.korean.utils.Hangul
import org.junit.jupiter.api.Test
import java.io.Serializable

/** 공개 한국어 결과 모델이 명시적인 직렬화 버전을 유지하는지 검증합니다. */
class SerializableModelContractTest: TestBase() {

    @Test
    fun `public serializable Korean models declare serial version uid`() {
        listOf(
            KoreanToken::class.java,
            KoreanChunk::class.java,
            Sentence::class.java,
            TokenizerProfile::class.java,
            KoreanPhrase::class.java,
            Hangul.HangulChar::class.java,
            Hangul.DoubleCoda::class.java,
        ).forEach(::assertSerialVersionUid)
    }

    private fun assertSerialVersionUid(type: Class<*>) {
        check(Serializable::class.java.isAssignableFrom(type)) {
            "${type.name} must remain Serializable"
        }
        val field = type.getDeclaredField("serialVersionUID")
        check(field.type == Long::class.javaPrimitiveType) {
            "${type.name}.serialVersionUID must be a primitive long"
        }
    }
}
