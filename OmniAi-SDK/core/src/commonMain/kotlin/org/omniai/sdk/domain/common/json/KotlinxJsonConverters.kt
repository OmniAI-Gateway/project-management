package org.omniai.sdk.domain.common.json

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

fun JsonValue.toKotlinxJsonElement(): JsonElement =
    when (this) {
        is JsonValue.JsonObject -> toKotlinxJsonObject()
        is JsonValue.JsonArray -> JsonArray(items.map { it.toKotlinxJsonElement() })
        is JsonValue.JsonString -> JsonPrimitive(value)
        is JsonValue.JsonNumber -> JsonPrimitive(value)
        is JsonValue.JsonBoolean -> JsonPrimitive(value)
        JsonValue.JsonNull -> JsonNull
    }

fun JsonValue.JsonObject.toKotlinxJsonObject(): JsonObject =
    JsonObject(properties.mapValues { (_, value) -> value.toKotlinxJsonElement() })

fun JsonObject.toDomainJsonObject(): JsonValue.JsonObject =
    JsonValue.JsonObject(properties = mapValues { (_, value) -> value.toDomainJsonValue() })

fun JsonElement.toDomainJsonValue(): JsonValue =
    when (this) {
        is JsonObject -> JsonValue.JsonObject(properties = mapValues { (_, value) -> value.toDomainJsonValue() })
        is JsonArray -> JsonValue.JsonArray(items = map { it.toDomainJsonValue() })
        is JsonPrimitive -> when {
            isString -> JsonValue.JsonString(content)
            content == "true" -> JsonValue.JsonBoolean(true)
            content == "false" -> JsonValue.JsonBoolean(false)
            content.toLongOrNull() != null -> JsonValue.JsonNumber(content.toLong())
            content.toDoubleOrNull() != null -> JsonValue.JsonNumber(content.toDouble())
            else -> JsonValue.JsonString(content)
        }
        JsonNull -> JsonValue.JsonNull
    }

