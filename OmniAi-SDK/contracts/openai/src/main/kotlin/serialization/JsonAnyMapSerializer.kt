package org.omniaigateway.contracts.openai.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

object JsonAnyMapSerializer : KSerializer<Map<String, Any?>> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("JsonAnyMap")

    override fun serialize(encoder: Encoder, value: Map<String, Any?>) {
        val jsonEncoder = encoder as? JsonEncoder
            ?: throw SerializationException("JsonAnyMapSerializer only works with JSON")
        val content = value.mapValues { (_, mapValue) -> mapValue.toJsonElement() }
        jsonEncoder.encodeJsonElement(JsonObject(content))
    }

    override fun deserialize(decoder: Decoder): Map<String, Any?> {
        val jsonDecoder = decoder as? JsonDecoder
            ?: throw SerializationException("JsonAnyMapSerializer only works with JSON")
        val element = jsonDecoder.decodeJsonElement()
        if (element !is JsonObject) {
            throw SerializationException("Expected JSON object for JsonAnyMap")
        }
        return element.mapValues { (_, jsonElement) -> jsonElement.toAnyValue() }
    }
}

private fun Any?.toJsonElement(): JsonElement =
    when (this) {
        null -> JsonNull
        is JsonElement -> this
        is String -> JsonPrimitive(this)
        is Number -> JsonPrimitive(this)
        is Boolean -> JsonPrimitive(this)
        is Map<*, *> -> JsonObject(entries.associate { (key, value) -> key.toString() to value.toJsonElement() })
        is List<*> -> JsonArray(map { it.toJsonElement() })
        else -> JsonPrimitive(toString())
    }

private fun JsonElement.toAnyValue(): Any? =
    when (this) {
        JsonNull -> null
        is JsonObject -> mapValues { (_, value) -> value.toAnyValue() }
        is JsonArray -> map { it.toAnyValue() }
        is JsonPrimitive -> when {
            isString -> content
            content == "true" -> true
            content == "false" -> false
            content.toLongOrNull() != null -> content.toLong()
            content.toDoubleOrNull() != null -> content.toDouble()
            else -> content
        }
    }

