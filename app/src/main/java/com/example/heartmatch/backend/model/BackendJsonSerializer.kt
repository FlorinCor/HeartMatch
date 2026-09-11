package com.example.heartmatch.backend.model

import com.example.heartmatch.engine.loader.JsonArray
import com.example.heartmatch.engine.loader.JsonElement
import com.example.heartmatch.engine.loader.JsonNull
import com.example.heartmatch.engine.loader.JsonObject
import com.example.heartmatch.engine.loader.JsonPrimitive

/**
 * Utility for formatting JSON structures and values.
 */
object BackendJsonSerializer {

    fun serialize(element: JsonElement): String {
        return when (element) {
            is JsonObject -> {
                val entries = element.members.map { (k, v) ->
                    "\"${escape(k)}\":${serialize(v)}"
                }
                "{${entries.joinToString(",")}}"
            }
            is JsonArray -> {
                val items = element.elements.map { serialize(it) }
                "[${items.joinToString(",")}]"
            }
            is JsonPrimitive -> {
                if (element.isString) {
                    "\"${escape(element.content)}\""
                } else {
                    element.content
                }
            }
            is JsonNull -> "null"
        }
    }

    fun escape(s: String): String {
        val sb = StringBuilder()
        for (c in s) {
            when (c) {
                '"' -> sb.append("\\\"")
                '\\' -> sb.append("\\\\")
                '\b' -> sb.append("\\b")
                '\u000C' -> sb.append("\\f")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                else -> {
                    if (c < ' ') {
                        sb.append(String.format("\\u%04x", c.code))
                    } else {
                        sb.append(c)
                    }
                }
            }
        }
        return sb.toString()
    }

    fun mapToJson(map: Map<String, Any?>): JsonObject {
        val members = mutableMapOf<String, JsonElement>()
        for ((key, value) in map) {
            members[key] = anyToJson(value)
        }
        return JsonObject(members)
    }

    fun listToJson(list: List<Any?>): JsonArray {
        return JsonArray(list.map { anyToJson(it) })
    }

    fun anyToJson(value: Any?): JsonElement {
        return when (value) {
            null -> JsonNull
            is JsonElement -> value
            is String -> JsonPrimitive(value, isString = true)
            is Number -> JsonPrimitive(value.toString(), isString = false)
            is Boolean -> JsonPrimitive(value.toString(), isString = false)
            is Map<*, *> -> {
                val members = mutableMapOf<String, JsonElement>()
                for ((k, v) in value) {
                    members[k.toString()] = anyToJson(v)
                }
                JsonObject(members)
            }
            is Iterable<*> -> JsonArray(value.map { anyToJson(it) })
            is Array<*> -> JsonArray(value.map { anyToJson(it) })
            else -> JsonPrimitive(value.toString(), isString = true)
        }
    }

    fun toJsonString(value: Any?): String {
        return serialize(anyToJson(value))
    }
}
