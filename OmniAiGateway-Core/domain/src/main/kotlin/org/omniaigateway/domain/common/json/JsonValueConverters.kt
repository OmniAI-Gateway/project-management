package org.omniaigateway.domain.common.json

fun Any?.toJsonValue(): JsonValue =
    when (this) {
        null -> JsonValue.JsonNull
        is JsonValue -> this
        is String -> JsonValue.JsonString(this)
        is Boolean -> JsonValue.JsonBoolean(this)
        is Number -> JsonValue.JsonNumber(this)
        is Map<*, *> -> JsonValue.JsonObject(
            properties = entries.associate { (key, value) ->
                key.toString() to value.toJsonValue()
            }
        )
        is List<*> -> JsonValue.JsonArray(items = map { it.toJsonValue() })
        else -> JsonValue.JsonString(toString())
    }

fun Map<String, Any?>.toJsonObject(): JsonValue.JsonObject =
    JsonValue.JsonObject(properties = mapValues { (_, value) -> value.toJsonValue() })

fun JsonValue.toRawAny(): Any? =
    when (this) {
        is JsonValue.JsonNull -> null
        is JsonValue.JsonString -> value
        is JsonValue.JsonBoolean -> value
        is JsonValue.JsonNumber -> value
        is JsonValue.JsonArray -> items.map { it.toRawAny() }
        is JsonValue.JsonObject -> properties.mapValues { (_, value) -> value.toRawAny() }
    }

fun JsonValue.JsonObject.toRawMap(): Map<String, Any?> =
    properties.mapValues { (_, value) -> value.toRawAny() }

