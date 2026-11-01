package com.nuvio.core.update

/**
 * Lightweight, pure Kotlin JSON parser that runs seamlessly on both Android and JVM
 * without relying on Android SDK stubs.
 */
class SimpleJsonParser(private val src: String) {

    private var cursor = 0

    fun parse(): Any? {
        skipWhitespace()
        if (cursor >= src.length) return null
        return parseValue()
    }

    private fun parseValue(): Any? {
        skipWhitespace()
        if (cursor >= src.length) return null

        return when (src[cursor]) {
            '{' -> parseObject()
            '[' -> parseArray()
            '"' -> parseString()
            't', 'f' -> parseBoolean()
            'n' -> parseNull()
            else -> parseNumber()
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun parseObject(): Map<String, Any?> {
        val map = mutableMapOf<String, Any?>()
        expect('{')
        skipWhitespace()

        if (peek() == '}') {
            cursor++
            return map
        }

        while (cursor < src.length) {
            skipWhitespace()
            val key = parseString()
            skipWhitespace()
            expect(':')
            val value = parseValue()
            map[key] = value

            skipWhitespace()
            val next = peek()
            if (next == ',') {
                cursor++
            } else if (next == '}') {
                cursor++
                break
            } else {
                break
            }
        }
        return map
    }

    @Suppress("UNCHECKED_CAST")
    fun parseArray(): List<Any?> {
        val list = mutableListOf<Any?>()
        expect('[')
        skipWhitespace()

        if (peek() == ']') {
            cursor++
            return list
        }

        while (cursor < src.length) {
            skipWhitespace()
            val value = parseValue()
            list.add(value)

            skipWhitespace()
            val next = peek()
            if (next == ',') {
                cursor++
            } else if (next == ']') {
                cursor++
                break
            } else {
                break
            }
        }
        return list
    }

    private fun parseString(): String {
        expect('"')
        val sb = StringBuilder()
        while (cursor < src.length) {
            val c = src[cursor++]
            if (c == '"') {
                return sb.toString()
            } else if (c == '\\' && cursor < src.length) {
                when (val esc = src[cursor++]) {
                    '"' -> sb.append('"')
                    '\\' -> sb.append('\\')
                    '/' -> sb.append('/')
                    'b' -> sb.append('\b')
                    'f' -> sb.append('\u000c')
                    'n' -> sb.append('\n')
                    'r' -> sb.append('\r')
                    't' -> sb.append('\t')
                    'u' -> {
                        if (cursor + 4 <= src.length) {
                            val hex = src.substring(cursor, cursor + 4)
                            sb.append(hex.toInt(16).toChar())
                            cursor += 4
                        }
                    }
                    else -> sb.append(esc)
                }
            } else {
                sb.append(c)
            }
        }
        return sb.toString()
    }

    private fun parseBoolean(): Boolean {
        return if (src.startsWith("true", cursor)) {
            cursor += 4
            true
        } else if (src.startsWith("false", cursor)) {
            cursor += 5
            false
        } else {
            cursor++
            false
        }
    }

    private fun parseNull(): Any? {
        if (src.startsWith("null", cursor)) {
            cursor += 4
        }
        return null
    }

    private fun parseNumber(): Number {
        val start = cursor
        if (src[cursor] == '-') cursor++
        while (cursor < src.length && (src[cursor].isDigit() || src[cursor] == '.' || src[cursor] == 'e' || src[cursor] == 'E' || src[cursor] == '+')) {
            cursor++
        }
        val text = src.substring(start, cursor)
        return if (text.contains('.')) {
            text.toDoubleOrNull() ?: 0.0
        } else {
            text.toLongOrNull() ?: 0L
        }
    }

    private fun skipWhitespace() {
        while (cursor < src.length && src[cursor].isWhitespace()) {
            cursor++
        }
    }

    private fun peek(): Char = if (cursor < src.length) src[cursor] else '\u0000'

    private fun expect(c: Char) {
        if (cursor < src.length && src[cursor] == c) {
            cursor++
        }
    }
}
