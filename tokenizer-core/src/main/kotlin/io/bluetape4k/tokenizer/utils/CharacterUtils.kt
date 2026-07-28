package io.bluetape4k.tokenizer.utils

import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.requireGe
import io.bluetape4k.support.requireZeroOrPositiveNumber
import io.bluetape4k.support.requireInRange
import java.io.Reader
import java.io.Serializable

/**
 * Unicode code point를 인식하는 character operation을 일관되게 제공하는 abstract utility입니다.
 *
 * ## 동작 계약
 * - 기본 implementation은 `Java5CharacterUtils` singleton입니다.
 * - Surrogate pair를 고려하는 read/transform API를 포함합니다.
 * - Buffer 기반 read 중 trailing high surrogate가 있으면 보존했다가 다음 호출 앞에 붙입니다.
 *
 * ```kotlin
 * val utils = CharacterUtils.getInstance()
 * val buffer = CharacterUtils.newCharacterBuffer(8)
 * // buffer.length == 0
 * // utils.codePointCount("hello") == 5
 * ```
 */
abstract class CharacterUtils: Serializable {

    companion object : KLogging() {
        private const val serialVersionUID = 1L

        @JvmStatic
        private val JAVA_5: CharacterUtils = Java5CharacterUtils()

        @JvmStatic
        /**
         * 기본 runtime [CharacterUtils] implementation instance를 반환합니다.
         *
         * ## 동작 계약
         * - 항상 같은 singleton instance를 반환합니다.
         * - 현재 implementation은 `Java5CharacterUtils`로 고정되어 있습니다.
         *
         * @return 기본 character utility singleton입니다.
         *
         * ```kotlin
         * val one = CharacterUtils.getInstance()
         * val two = CharacterUtils.getInstance()
         * // one === two
         * ```
         */
        fun getInstance(): CharacterUtils = JAVA_5

        @JvmStatic
        /**
         * 지정한 capacity를 가진 새 [CharacterBuffer]를 만듭니다.
         *
         * ## 동작 계약
         * - `bufferSize >= 2`를 요구하며, 아니면 [IllegalArgumentException]을 던집니다.
         * - Buffer content는 비어 있고 `offset`과 `length`는 0으로 초기화됩니다.
         *
         * @param bufferSize 생성할 backing char array size입니다. Surrogate pair 처리를 위해 2 이상이어야 합니다.
         * @return 비어 있는 새 [CharacterBuffer]입니다.
         *
         * ```kotlin
         * val buffer = CharacterUtils.newCharacterBuffer(4)
         * // buffer.buffer.size == 4
         * // buffer.length == 0
         * ```
         */
        fun newCharacterBuffer(bufferSize: Int): CharacterBuffer {
            require(bufferSize >= 2) { "buffer size must be >= 2" }
            return CharacterBuffer(CharArray(bufferSize), 0, 0)
        }

        @JvmStatic
        /**
         * [reader]에서 최대 [len]개 character를 읽어 [dest]의 [offset]부터 채웁니다.
         *
         * ## 동작 계약
         * - [len]개 character를 채우거나 EOF(`-1`)에 도달할 때까지 read를 반복합니다.
         * - 실제로 읽은 character 수를 반환합니다.
         * - Partial read가 발생해도 [dest]에 이미 쓴 data는 보존합니다.
         *
         * @param reader character를 읽을 source reader입니다.
         * @param dest 읽은 character를 저장할 destination array입니다.
         * @param offset [dest]에 쓰기 시작할 index입니다.
         * @param len 읽으려는 최대 character 수입니다.
         * @return 실제로 읽어 [dest]에 쓴 character 수입니다.
         *
         * ```kotlin
         * val out = CharArray(5)
         * val read = CharacterUtils.readFully("abc".reader(), out, 0, 5)
         * // read == 3
         * // String(out, 0, read) == "abc"
         * ```
         */
        fun readFully(reader: Reader, dest: CharArray, offset: Int, len: Int): Int {
            var read = 0
            while (read < len) {
                val r = reader.read(dest, offset + read, len - read)
                if (r == -1) {
                    break
                }
                read += r
            }
            return read
        }
    }

    /**
     * [seq]의 [offset] 위치에 있는 code point를 반환합니다.
     *
     * ## 동작 계약
     * - Implementation은 code point를 계산할 때 surrogate pair를 고려합니다.
     * - [offset]은 sequence 안의 시작 char index입니다.
     *
     * @param seq code point를 읽을 character sequence입니다.
     * @param offset 읽기 시작할 char index입니다.
     * @return [offset] 위치의 Unicode code point입니다.
     *
     * ```kotlin
     * val cp = CharacterUtils.getInstance().codePointAt("abc", 1)
     * // cp == 'b'.code
     * ```
     */
    abstract fun codePointAt(seq: CharSequence, offset: Int = 0): Int

    /**
     * [chars]의 range `[offset, limit)` 안에서 [offset] 위치의 code point를 반환합니다.
     *
     * ## 동작 계약
     * - [limit]은 lookup의 exclusive upper boundary입니다.
     * - Input에 surrogate pair가 있어도 하나의 code point로 반환합니다.
     *
     * @param chars code point를 읽을 char array입니다.
     * @param offset 읽기 시작할 char index입니다.
     * @param limit lookup에 허용할 exclusive upper boundary입니다.
     * @return [offset] 위치의 Unicode code point입니다.
     *
     * ```kotlin
     * val chars = "A".toCharArray()
     * val cp = CharacterUtils.getInstance().codePointAt(chars, 0, chars.size)
     * // Character.isValidCodePoint(cp) == true
     * ```
     */
    abstract fun codePointAt(chars: CharArray, offset: Int, limit: Int): Int

    /**
     * [seq]에 들어 있는 Unicode code point 수를 반환합니다.
     *
     * ## 동작 계약
     * - Implementation은 surrogate pair를 하나의 code point로 셉니다.
     * - Supplementary character가 있으면 반환값이 `seq.length`와 다를 수 있습니다.
     *
     * @param seq code point 수를 셀 character sequence입니다.
     * @return [seq]의 Unicode code point 개수입니다.
     *
     * ```kotlin
     * val count = CharacterUtils.getInstance().codePointCount("A😀")
     * // count == 2
     * ```
     */
    abstract fun codePointCount(seq: CharSequence): Int

    /**
     * `buffer[offset, limit)`의 character를 code point 단위로 in-place lowercase합니다.
     *
     * ## 동작 계약
     * - [limit]이 buffer size를 넘으면 exception을 던집니다.
     * - [buffer]를 직접 수정하며 새 array를 할당하지 않습니다.
     * - Index는 `Character.toChars`가 반환하는 char count만큼 전진합니다.
     *
     * @param buffer lowercase 결과를 직접 쓸 char array입니다.
     * @param offset 변환을 시작할 char index입니다.
     * @param limit 변환할 range의 exclusive upper boundary입니다.
     *
     * ```kotlin
     * val chars = "ABC".toCharArray()
     * CharacterUtils.getInstance().toLowerCase(chars, 0, chars.size)
     * // String(chars) == "abc"
     * ```
     */
    fun toLowerCase(buffer: CharArray, offset: Int, limit: Int) {
        buffer.size.requireGe(limit, "buffer size")
        offset.requireInRange(0, buffer.size, "offset")

        var i = offset
        while (i < limit) {
            i += Character.toChars(Character.toLowerCase(codePointAt(buffer, i, limit)), buffer, i)
        }
    }

    /**
     * `buffer[offset, limit)`의 character를 code point 단위로 in-place uppercase합니다.
     *
     * ## 동작 계약
     * - [limit]과 [offset]을 buffer bounds에 대해 검증합니다.
     * - 결과를 같은 [buffer]에 다시 씁니다.
     *
     * @param buffer uppercase 결과를 직접 쓸 char array입니다.
     * @param offset 변환을 시작할 char index입니다.
     * @param limit 변환할 range의 exclusive upper boundary입니다.
     *
     * ```kotlin
     * val chars = "abc".toCharArray()
     * CharacterUtils.getInstance().toUpperCase(chars, 0, chars.size)
     * // String(chars) == "ABC"
     * ```
     */
    fun toUpperCase(buffer: CharArray, offset: Int, limit: Int) {
        buffer.size.requireGe(limit, "buffer size")
        offset.requireInRange(0, buffer.size, "offset")

        var i = offset
        while (i < limit) {
            i += Character.toChars(Character.toUpperCase(codePointAt(buffer, i, limit)), buffer, i)
        }
    }

    /**
     * Char-array slice를 code point로 변환해 [dest]에 씁니다.
     *
     * ## 동작 계약
     * - [srcLen]은 0 이상이어야 하며, 음수이면 [IllegalArgumentException]을 던집니다.
     * - 각 code point는 `dest[destOff + index]`에 순서대로 씁니다.
     * - 쓴 code point 수를 반환합니다.
     *
     * @param src 변환할 source char array입니다.
     * @param srcOff source slice 시작 offset입니다.
     * @param srcLen 변환할 source char 수입니다.
     * @param dest code point를 쓸 destination int array입니다.
     * @param destOff destination write 시작 offset입니다.
     * @return [dest]에 쓴 code point 수입니다.
     *
     * ```kotlin
     * val out = IntArray(4)
     * val count = CharacterUtils.getInstance().toCodePoints("ab".toCharArray(), 0, 2, out, 0)
     * // count == 2
     * // out[0] == 'a'.code
     * ```
     */
    fun toCodePoints(src: CharArray, srcOff: Int, srcLen: Int, dest: IntArray, destOff: Int): Int {
        srcLen.requireZeroOrPositiveNumber("srcLen")

        var codePointCount = 0
        var i = 0
        while (i < srcLen) {
            val cp = codePointAt(src, srcOff + i, srcOff + srcLen)
            val charCount = Character.charCount(cp)
            dest[destOff + codePointCount++] = cp
            i += charCount
        }
        return codePointCount
    }

    /**
     * Code-point array slice를 UTF-16 char로 변환해 [dest]에 씁니다.
     *
     * ## 동작 계약
     * - [srcLen]은 0 이상이어야 합니다.
     * - 각 code point는 `Character.toChars`로 변환해 [dest]에 append합니다.
     * - 쓴 char 수, 즉 UTF-16 code unit 수를 반환합니다.
     *
     * @param src 변환할 source code-point array입니다.
     * @param srcOff source slice 시작 offset입니다.
     * @param srcLen 변환할 source code-point 수입니다.
     * @param dest UTF-16 char를 쓸 destination array입니다.
     * @param destOff destination write 시작 offset입니다.
     * @return [dest]에 쓴 UTF-16 char 수입니다.
     *
     * ```kotlin
     * val dest = CharArray(4)
     * val written = CharacterUtils.getInstance().toChars(intArrayOf('a'.code, 'b'.code), 0, 2, dest, 0)
     * // written == 2
     * // String(dest, 0, written) == "ab"
     * ```
     */
    fun toChars(src: IntArray, srcOff: Int, srcLen: Int, dest: CharArray, destOff: Int): Int {
        srcLen.requireZeroOrPositiveNumber("srcLen")

        var written = 0
        for (i in 0 until srcLen) {
            written += Character.toChars(src[srcOff + i], dest, destOff + written)
        }
        return written
    }

    /**
     * [reader]에서 character를 읽어 [buffer]에 최대 [numChars]개까지 채웁니다.
     *
     * ## 동작 계약
     * - 정확히 [numChars]개 character를 buffer에 넣으면 `true`를 반환합니다.
     * - Implementation은 surrogate-pair boundary를 보존하기 위해 trailing high surrogate를 보관할 수 있습니다.
     *
     * @param buffer 읽은 character를 담을 [CharacterBuffer]입니다.
     * @param reader character를 읽을 source reader입니다.
     * @param numChars 채우려는 character 수입니다. 기본값은 backing buffer size입니다.
     * @return [numChars]개를 채웠으면 `true`, EOF 등으로 덜 채웠으면 `false`입니다.
     *
     * ```kotlin
     * val utils = CharacterUtils.getInstance()
     * val buffer = CharacterUtils.newCharacterBuffer(4)
     * val full = utils.fill(buffer, "ab".reader(), 2)
     * // full == true
     * ```
     */
    abstract fun fill(buffer: CharacterBuffer, reader: Reader, numChars: Int = buffer.buffer.size): Boolean

    /**
     * `buf[start, start+count)` 안에서 [index]부터 [offset] code point만큼 이동한 char index를 반환합니다.
     *
     * ## 동작 계약
     * - Implementation은 character boundary를 지키며 valid UTF-16 index를 반환합니다.
     *
     * @param buf 기준 char array입니다.
     * @param start 검색 range의 시작 index입니다.
     * @param count 검색 range에 포함할 char 수입니다.
     * @param index 이동을 시작할 char index입니다.
     * @param offset 이동할 code point 수입니다.
     * @return code point 단위 이동 후 도달한 UTF-16 char index입니다.
     *
     * ```kotlin
     * val chars = "abcd".toCharArray()
     * val index = CharacterUtils.getInstance().offsetByCodePoints(chars, 0, chars.size, 1, 2)
     * // index == 3
     * ```
     */
    abstract fun offsetByCodePoints(buf: CharArray, start: Int, count: Int, index: Int, offset: Int): Int

    private class Java5CharacterUtils: CharacterUtils() {

        override fun codePointAt(seq: CharSequence, offset: Int): Int {
            return Character.codePointAt(seq, offset)
        }

        override fun codePointAt(chars: CharArray, offset: Int, limit: Int): Int {
            return Character.codePointAt(chars, offset, limit)
        }

        override fun fill(buffer: CharacterBuffer, reader: Reader, numChars: Int): Boolean {
            buffer.buffer.size.requireGe(2, "buffer size")
            numChars.requireInRange(2, buffer.buffer.size, "numChars")
            // require(numChars in 2..buffer.buffer.size) { "numCharrs must be 2 .. buffer size" }

            val charBuffer = buffer.buffer
            buffer.offset = 0

            // 이전 호출에서 저장한 ending high surrogate를 먼저 설치한다.
            val offset = if (buffer.lastTrailingHighSurrogate != 0.toChar()) {
                charBuffer[0] = buffer.lastTrailingHighSurrogate
                buffer.lastTrailingHighSurrogate = 0.toChar()
                1
            } else {
                0
            }

            val read = readFully(reader, charBuffer, offset, numChars - offset)

            buffer.length = offset + read
            val result = buffer.length == numChars
            if (buffer.length < numChars) {
                // Buffer를 끝까지 채우지 못했다. 마지막 char가 high surrogate여도 더 처리할 수 없다.
                return result
            }

            if (Character.isHighSurrogate(charBuffer[buffer.length - 1])) {
                buffer.lastTrailingHighSurrogate = charBuffer[--buffer.length]
            }

            return result
        }

        override fun codePointCount(seq: CharSequence): Int {
            return Character.codePointCount(seq, 0, seq.length)
        }

        override fun offsetByCodePoints(buf: CharArray, start: Int, count: Int, index: Int, offset: Int): Int {
            return Character.offsetByCodePoints(buf, start, count, index, offset)
        }
    }

    private class Java4CharacterUtils: CharacterUtils() {

        override fun codePointAt(seq: CharSequence, offset: Int): Int {
            return seq[offset].code
        }

        override fun codePointAt(chars: CharArray, offset: Int, limit: Int): Int {
            require(offset < limit) { "offset[$offset] must be less than limit[$limit]" }
            return chars[offset].code
        }

        override fun fill(buffer: CharacterBuffer, reader: Reader, numChars: Int): Boolean {
            require(buffer.buffer.size >= 1)
            require(numChars in 1..buffer.buffer.size) {
                "numChars must be 1 .. the buffer size[${buffer.buffer.size}]"
            }

            buffer.offset = 0
            val read = readFully(reader, buffer.buffer, 0, numChars)
            buffer.length = read
            buffer.lastTrailingHighSurrogate = 0.toChar()
            return read == numChars
        }

        override fun codePointCount(seq: CharSequence): Int {
            return seq.length
        }

        override fun offsetByCodePoints(buf: CharArray, start: Int, count: Int, index: Int, offset: Int): Int {
            val result = index + offset
            check(result in 0..count) { "index[$index]+offset[$offset] must be 0 .. count[$count]" }
            return result
        }

    }

    /**
     * Char array와 현재 offset, length, trailing-surrogate state를 함께 보관하는 container입니다.
     *
     * ## 동작 계약
     * - [buffer]는 read와 transform에 사용하는 mutable backing array입니다.
     * - [offset]과 [length]는 internal operation이 갱신하므로 외부에서는 read-only처럼 다룹니다.
     * - 재사용하려면 [reset]을 호출해 state를 다시 초기화합니다.
     *
     * @property buffer character data를 담는 mutable backing array입니다.
     *
     * ```kotlin
     * val buffer = CharacterUtils.newCharacterBuffer(6)
     * buffer.reset()
     * // buffer.offset == 0
     * // buffer.length == 0
     * ```
     */
    class CharacterBuffer private constructor(
        val buffer: CharArray,
    ) {

        companion object {
            @JvmStatic
            /**
             * 기존 char array를 감싸는 [CharacterBuffer]를 만듭니다.
             *
             * ## 동작 계약
             * - 제공된 argument로 [offset]과 [length]를 직접 설정합니다.
             * - Array reference를 공유하며 copy하지 않습니다.
             *
             * @param buffer 감쌀 source char array입니다.
             * @param offset 현재 valid data의 시작 offset입니다.
             * @param length 현재 valid data의 character 수입니다.
             * @return [buffer] reference를 공유하는 [CharacterBuffer]입니다.
             *
             * ```kotlin
             * val raw = CharArray(4)
             * val buffer = CharacterUtils.CharacterBuffer(raw, offset = 1, length = 2)
             * // buffer.offset == 1
             * // buffer.length == 2
             * ```
             */
            operator fun invoke(buffer: CharArray, offset: Int = 0, length: Int = 0): CharacterBuffer {
                return CharacterBuffer(buffer).apply {
                    this.offset = offset
                    this.length = length
                }
            }
        }

        /**
         * Buffer 안에서 현재 valid data가 시작되는 offset입니다.
         *
         * ## 동작 계약
         * - Internal logic만 갱신하므로 외부에서는 read-only처럼 다룹니다.
         * - [reset]이 0으로 되돌립니다.
         *
         * ```kotlin
         * val buffer = CharacterUtils.newCharacterBuffer(4)
         * buffer.reset()
         * // buffer.offset == 0
         * ```
         */
        var offset: Int = 0
            internal set

        /**
         * Buffer에 현재 채워진 valid character 수입니다.
         *
         * ## 동작 계약
         * - 각 [fill] 호출 후 읽은 character 수를 반영하도록 갱신됩니다.
         * - [reset]이 0으로 되돌립니다.
         *
         * ```kotlin
         * val buffer = CharacterUtils.newCharacterBuffer(4)
         * // buffer.length == 0
         * ```
         */
        var length: Int = 0
            internal set

        /**
         * 다음 read 앞에 붙일 trailing high surrogate를 저장합니다.
         *
         * ## 동작 계약
         * - Pending surrogate가 없으면 `0.toChar()`를 보관합니다.
         * - [reset]이 `0.toChar()`로 되돌립니다.
         *
         * ```kotlin
         * val buffer = CharacterUtils.newCharacterBuffer(4)
         * // buffer.lastTrailingHighSurrogate == 0.toChar()
         * ```
         */
        var lastTrailingHighSurrogate: Char = 0.toChar()

        /**
         * Buffer state를 initial value로 reset합니다.
         *
         * ## 동작 계약
         * - [offset], [length], [lastTrailingHighSurrogate]를 default로 되돌립니다.
         * - [buffer] content는 보존하고 metadata만 clear합니다.
         *
         * ```kotlin
         * val buffer = CharacterUtils.newCharacterBuffer(4)
         * buffer.reset()
         * // buffer.length == 0
         * ```
         */
        fun reset() {
            offset = 0
            length = 0
            lastTrailingHighSurrogate = 0.toChar()
        }
    }
}
