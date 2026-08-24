package io.bluetape4k.lingua

import com.github.pemistahl.lingua.api.Language
import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeTrue
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable

class LanguageSegmentContractTest: AbstractLinguaTest() {

    @Test
    fun `segment validates UTF-16 range and confidence`() {
        assertFailsWith<IllegalArgumentException> {
            LanguageSegment(-1, 2, Language.ENGLISH, 0.9)
        }
        assertFailsWith<IllegalArgumentException> {
            LanguageSegment(2, 2, Language.ENGLISH, 0.9)
        }
        assertFailsWith<IllegalArgumentException> {
            LanguageSegment(0, 2, Language.ENGLISH, -0.1)
        }
        assertFailsWith<IllegalArgumentException> {
            LanguageSegment(0, 2, Language.ENGLISH, 1.1)
        }
    }

    @Test
    fun `copy revalidates public invariants`() {
        val segment = LanguageSegment(0, 2, Language.ENGLISH, 0.9)

        assertFailsWith<IllegalArgumentException> { segment.copy(start = -1) }
        assertFailsWith<IllegalArgumentException> { segment.copy(endExclusive = 0) }
        assertFailsWith<IllegalArgumentException> { segment.copy(confidence = 1.1) }
    }

    @Test
    fun `segment is serializable with stable value`() {
        val segment = LanguageSegment(2, 7, Language.ENGLISH, 0.9)
        Serializable::class.java.isInstance(segment).shouldBeTrue()

        val bytes = ByteArrayOutputStream()
        ObjectOutputStream(bytes).use { it.writeObject(segment) }
        val restored = ObjectInputStream(ByteArrayInputStream(bytes.toByteArray())).use {
            it.readObject() as LanguageSegment
        }

        restored shouldBeEqualTo segment
    }
}
