package com.example.heartmatch.engine.loader

/**
 * Lightweight, zero-dependency JSON parser and serializer.
 */
sealed class JsonElement {
    open fun asObject(): JsonObject = this as? JsonObject ?: error("Not a JsonObject: $this")
    open fun asArray(): JsonArray = this as? JsonArray ?: error("Not a JsonArray: $this")
    open fun asPrimitive(): JsonPrimitive = this as? JsonPrimitive ?: error("Not a JsonPrimitive: $this")
    open fun asString(): String = asPrimitive().content
    open fun asInt(): Int = asPrimitive().content.toInt()
    open fun asLong(): Long = asPrimitive().content.toLong()
    open fun asDouble(): Double = asPrimitive().content.toDouble()
    open fun asBoolean(): Boolean = asPrimitive().content.toBoolean()
}

data class JsonObject(val members: Map<String, JsonElement>) : JsonElement() {
    operator fun get(key: String): JsonElement? = members[key]
    fun getString(key: String): String? = members[key]?.asPrimitive()?.content
    fun getInt(key: String): Int? = members[key]?.asPrimitive()?.content?.toIntOrNull()
    fun getLong(key: String): Long? = members[key]?.asPrimitive()?.content?.toLongOrNull()
    fun getDouble(key: String): Double? = members[key]?.asPrimitive()?.content?.toDoubleOrNull()
    fun getBoolean(key: String): Boolean? = members[key]?.asPrimitive()?.content?.toBooleanStrictOrNull()
    fun getObject(key: String): JsonObject? = members[key]?.asObject()
    fun getArray(key: String): JsonArray? = members[key]?.asArray()
}

data class JsonArray(val elements: List<JsonElement>) : JsonElement(), Iterable<JsonElement> {
    override fun iterator(): Iterator<JsonElement> = elements.iterator()
    val size: Int get() = elements.size
    operator fun get(index: Int): JsonElement = elements[index]
}

data class JsonPrimitive(val content: String, val isString: Boolean = false) : JsonElement()

object JsonNull : JsonElement()

class JsonParser(private val src: String) {
    private var pos = 0

    fun parse(): JsonElement {
        skipWhitespace()
        val result = parseValue()
        skipWhitespace()
        return result
    }

    private fun parseValue(): JsonElement {
        skipWhitespace()
        if (pos >= src.length) error("Unexpected end of JSON input at position $pos")

        return when (val ch = src[pos]) {
            '{' -> parseObject()
            '[' -> parseArray()
            '"' -> parseString()
            't', 'f' -> parseBoolean()
            'n' -> parseNull()
            '-', in '0'..'9' -> parseNumber()
            else -> error("Unexpected character '$ch' at position $pos")
        }
    }

    private fun parseObject(): JsonObject {
        consume('{')
        val map = mutableMapOf<String, JsonElement>()
        skipWhitespace()
        if (peek() == '}') {
            consume('}')
            return JsonObject(map)
        }

        while (true) {
            skipWhitespace()
            val key = parseString().content
            skipWhitespace()
            consume(':')
            val value = parseValue()
            map[key] = value
            skipWhitespace()
            when (peek()) {
                ',' -> {
                    consume(',')
                    continue
                }
                '}' -> {
                    consume('}')
                    break
                }
                else -> error("Expected ',' or '}' in object at position $pos")
            }
        }
        return JsonObject(map)
    }

    private fun parseArray(): JsonArray {
        consume('[')
        val list = mutableListOf<JsonElement>()
        skipWhitespace()
        if (peek() == ']') {
            consume(']')
            return JsonArray(list)
        }

        while (true) {
            val value = parseValue()
            list.add(value)
            skipWhitespace()
            when (peek()) {
                ',' -> {
                    consume(',')
                    continue
                }
                ']' -> {
                    consume(']')
                    break
                }
                else -> error("Expected ',' or ']' in array at position $pos")
            }
        }
        return JsonArray(list)
    }

    private fun parseString(): JsonPrimitive {
        consume('"')
        val sb = StringBuilder()
        while (pos < src.length) {
            val ch = src[pos++]
            if (ch == '"') {
                return JsonPrimitive(sb.toString(), isString = true)
            }
            if (ch == '\\') {
                if (pos >= src.length) error("Unterminated escape sequence at $pos")
                when (val esc = src[pos++]) {
                    '"' -> sb.append('"')
                    '\\' -> sb.append('\\')
                    '/' -> sb.append('/')
                    'b' -> sb.append('\b')
                    'f' -> sb.append('\u000C')
                    'n' -> sb.append('\n')
                    'r' -> sb.append('\r')
                    't' -> sb.append('\t')
                    'u' -> {
                        if (pos + 4 > src.length) error("Invalid unicode escape at $pos")
                        val hex = src.substring(pos, pos + 4)
                        pos += 4
                        sb.append(hex.toInt(16).toChar())
                    }
                    else -> error("Invalid escape char '$esc' at $pos")
                }
            } else {
                sb.append(ch)
            }
        }
        error("Unterminated string literal at $pos")
    }

    private fun parseNumber(): JsonPrimitive {
        val start = pos
        if (src[pos] == '-') pos++
        while (pos < src.length && (src[pos] in '0'..'9' || src[pos] == '.' || src[pos] == 'e' || src[pos] == 'E' || src[pos] == '+' || src[pos] == '-')) {
            pos++
        }
        return JsonPrimitive(src.substring(start, pos), isString = false)
    }

    private fun parseBoolean(): JsonPrimitive {
        if (src.startsWith("true", pos)) {
            pos += 4
            return JsonPrimitive("true", isString = false)
        }
        if (src.startsWith("false", pos)) {
            pos += 5
            return JsonPrimitive("false", isString = false)
        }
        error("Invalid boolean at position $pos")
    }

    private fun parseNull(): JsonElement {
        if (src.startsWith("null", pos)) {
            pos += 4
            return JsonNull
        }
        error("Invalid null at position $pos")
    }

    private fun skipWhitespace() {
        while (pos < src.length && (src[pos] == ' ' || src[pos] == '\t' || src[pos] == '\n' || src[pos] == '\r')) {
            pos++
        }
    }

    private fun peek(): Char {
        skipWhitespace()
        if (pos >= src.length) error("Unexpected end of input at position $pos")
        return src[pos]
    }

    private fun consume(expected: Char) {
        skipWhitespace()
        if (pos >= src.length || src[pos] != expected) {
            error("Expected '$expected' at position $pos but found '${if (pos < src.length) src[pos] else "EOF"}'")
        }
        pos++
    }

    companion object {
        fun parse(json: String): JsonElement = JsonParser(json).parse()
    }
}
