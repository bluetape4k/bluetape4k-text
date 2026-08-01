package io.bluetape4k.tokenizer.model

import io.bluetape4k.tokenizer.AbstractCoreTest
import io.bluetape4k.tokenizer.utils.CharArrayMap
import io.bluetape4k.tokenizer.utils.CharArraySet
import io.bluetape4k.tokenizer.utils.CharacterUtils
import org.junit.jupiter.api.Test
import java.io.Serializable

/** 공개 core Serializable 타입이 명시적인 직렬화 버전을 유지하는지 검증합니다. */
class SerializableModelContractTest: AbstractCoreTest() {

    @Test
    fun `public serializable core types declare serial version uid`() {
        listOf(
            BlockwordOptions::class.java,
            BlockwordRequest::class.java,
            BlockwordResponse::class.java,
            TokenizeOptions::class.java,
            TokenizeRequest::class.java,
            TokenizeResponse::class.java,
            AbstractMessage::class.java,
            CharArrayMap::class.java,
            CharArraySet::class.java,
            CharacterUtils::class.java,
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
