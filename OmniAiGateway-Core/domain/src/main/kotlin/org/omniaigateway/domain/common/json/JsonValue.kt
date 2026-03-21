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
}

typealias JsonObjectMap = Map<String, JsonValue>

