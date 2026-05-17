package io.bluetape4k.tokenizer.utils

import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.requireGe
import io.bluetape4k.support.requireZeroOrPositiveNumber
import io.bluetape4k.support.requireInRange
import java.io.Reader
import java.io.Serializable

/**
 * Abstract utility providing Unicode code-point-aware character operations in a consistent way.
 *
 * ## Behavior / Contract
 * - The default implementation is the `Java5CharacterUtils` singleton.
 * - Includes read/transform APIs that account for surrogate pairs.
 * - During buffer-based reads, a trailing high surrogate is preserved and prepended on the next call.
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
         * Returns the default runtime [CharacterUtils] implementation instance.
         *
         * ## Behavior / Contract
         * - Always returns the same singleton instance.
         * - The current implementation is fixed to `Java5CharacterUtils`.
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
         * Creates a new [CharacterBuffer] with the given capacity.
         *
         * ## Behavior / Contract
         * - Requires `bufferSize >= 2`; throws [IllegalArgumentException] otherwise.
         * - The buffer content is empty; `offset` and `length` are initialized to 0.
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
         * Reads up to [len] characters from [reader] into [dest] starting at [offset].
         *
         * ## Behavior / Contract
         * - Repeats reads until [len] characters are filled or EOF (`-1`) is reached.
         * - Returns the actual number of characters read.
         * - Data already placed in [dest] is preserved on partial reads.
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
     * Returns the code point at [offset] in [seq].
     *
     * ## Behavior / Contract
     * - Implementations account for surrogate pairs when computing the code point.
     * - [offset] is the starting char index within the sequence.
     *
     * ```kotlin
     * val cp = CharacterUtils.getInstance().codePointAt("abc", 1)
     * // cp == 'b'.code
     * ```
     */
    abstract fun codePointAt(seq: CharSequence, offset: Int = 0): Int

    /**
     * Returns the code point at [offset] within the range `[offset, limit)` of [chars].
     *
     * ## Behavior / Contract
     * - [limit] is the exclusive upper boundary for the lookup.
     * - Returns a single code point even when the input contains a surrogate pair.
     *
     * ```kotlin
     * val chars = "A".toCharArray()
     * val cp = CharacterUtils.getInstance().codePointAt(chars, 0, chars.size)
     * // Character.isValidCodePoint(cp) == true
     * ```
     */
    abstract fun codePointAt(chars: CharArray, offset: Int, limit: Int): Int

    /**
     * Returns the number of Unicode code points in [seq].
     *
     * ## Behavior / Contract
     * - Surrogate pairs are counted as a single code point by the implementation.
     * - The return value may differ from `seq.length` for supplementary characters.
     *
     * ```kotlin
     * val count = CharacterUtils.getInstance().codePointCount("A😀")
     * // count == 2
     * ```
     */
    abstract fun codePointCount(seq: CharSequence): Int

    /**
     * Lowercases characters in `buffer[offset, limit)` in-place, code-point by code-point.
     *
     * ## Behavior / Contract
     * - Throws if [limit] exceeds the buffer size.
     * - Modifies [buffer] directly; no new array is allocated.
     * - Index advances by the char count returned by `Character.toChars`.
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
     * Uppercases characters in `buffer[offset, limit)` in-place, code-point by code-point.
     *
     * ## Behavior / Contract
     * - Validates [limit] and [offset] against the buffer bounds.
     * - Writes the result back into the same [buffer].
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
     * Converts a char-array slice into code points and writes them into [dest].
     *
     * ## Behavior / Contract
     * - [srcLen] must be zero or positive; negative values throw [IllegalArgumentException].
     * - Each code point is written sequentially to `dest[destOff + index]`.
     * - Returns the number of code points written.
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
     * Converts a code-point array slice into UTF-16 chars and writes them into [dest].
     *
     * ## Behavior / Contract
     * - [srcLen] must be zero or positive.
     * - Each code point is converted via `Character.toChars` and appended to [dest].
     * - Returns the number of chars written (UTF-16 code units).
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
     * Reads characters from [reader] into [buffer], filling up to [numChars] characters.
     *
     * ## Behavior / Contract
     * - Returns `true` when exactly [numChars] characters were placed in the buffer.
     * - Implementations may retain a trailing high surrogate to preserve surrogate-pair boundaries.
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
     * Returns the char index reached by advancing [offset] code points from [index] within `buf[start, start+count)`.
     *
     * ## Behavior / Contract
     * - Implementations respect character boundaries and return a valid UTF-16 index.
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

            // Install the previously saved ending high surrogate:
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
                // We failed to fill the buffer. Even if the last char is a high
                // surrogate, there is nothing we can do
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
     * Container that holds a char array together with its current offset, length, and trailing-surrogate state.
     *
     * ## Behavior / Contract
     * - [buffer] is the mutable backing array used for reads and transforms.
     * - [offset] and [length] are updated by internal operations; treat them as read-only externally.
     * - Call [reset] to reinitialize state for reuse.
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
             * Creates a [CharacterBuffer] wrapping an existing char array.
             *
             * ## Behavior / Contract
             * - Sets [offset] and [length] directly from the provided arguments.
             * - The array reference is shared; no copy is made.
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
         * The start offset of the currently valid data in the buffer.
         *
         * ## Behavior / Contract
         * - Updated only by internal logic; treat as read-only externally.
         * - Reset to 0 by [reset].
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
         * Number of valid characters currently filled in the buffer.
         *
         * ## Behavior / Contract
         * - Updated after each [fill] call to reflect the number of characters read.
         * - Reset to 0 by [reset].
         *
         * ```kotlin
         * val buffer = CharacterUtils.newCharacterBuffer(4)
         * // buffer.length == 0
         * ```
         */
        var length: Int = 0
            internal set

        /**
         * Stores a trailing high surrogate to be prepended on the next read.
         *
         * ## Behavior / Contract
         * - Holds `0.toChar()` when there is no pending surrogate.
         * - Restored to `0.toChar()` by [reset].
         *
         * ```kotlin
         * val buffer = CharacterUtils.newCharacterBuffer(4)
         * // buffer.lastTrailingHighSurrogate == 0.toChar()
         * ```
         */
        var lastTrailingHighSurrogate: Char = 0.toChar()

        /**
         * Resets the buffer state to its initial values.
         *
         * ## Behavior / Contract
         * - Sets [offset], [length], and [lastTrailingHighSurrogate] back to their defaults.
         * - The contents of [buffer] are preserved; only the metadata is cleared.
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
