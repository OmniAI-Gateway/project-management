package org.omniaigateway.domain.common.json

/**
 * Internal JSON model used by the domain to avoid weak Any?-based payloads.
 */
sealed class JsonValue {
    data class JsonObject(val properties: Map<String, JsonValue>) : JsonValue()

    data class JsonArray(val items: List<JsonValue>) : JsonValue()

    data class JsonString(val value: String) : JsonValue()

    data class JsonNumber(val value: Number) : JsonValue()

    data class JsonBoolean(val value: Boolean) : JsonValue()

    data object JsonNull : JsonValue()


    fun toJsonString(): String = when (this) {
        is JsonNull -> "null"
        is JsonBoolean -> value.toString()
        is JsonNumber -> value.toString()
        is JsonString -> "\"$value\""

        is JsonArray -> items.joinToString(
            prefix = "[",
            separator = ",",
            postfix = "]"
        ) { item ->
            item.toJsonString()
        }

        is JsonObject -> properties.entries.joinToString(
            prefix = "{",
            separator = ",",
            postfix = "}"
        ) { (key, value) ->
            "\"$key\":${value.toJsonString()}"
        }
    }
}

typealias JsonObjectMap = Map<String, JsonValue>



