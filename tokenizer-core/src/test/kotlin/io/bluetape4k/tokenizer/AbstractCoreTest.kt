package io.bluetape4k.tokenizer

import io.bluetape4k.jackson3.Jackson
import io.bluetape4k.logging.KLogging
import net.datafaker.Faker
import tools.jackson.databind.json.JsonMapper
import java.util.*

abstract class AbstractCoreTest {

    companion object: KLogging() {
        const val REPEAT_SIZE = 5

        @JvmStatic
        protected val faker = Faker(Locale.getDefault())

        @JvmStatic
        protected val mapper: JsonMapper by lazy { Jackson.defaultJsonMapper }
    }
}
