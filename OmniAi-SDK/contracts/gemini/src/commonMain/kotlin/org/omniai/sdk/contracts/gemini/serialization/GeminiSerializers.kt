package org.omniai.sdk.contracts.gemini.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull

object StringAnyMapSerializer : KSerializer<Map<String, Any?>> {
    override val descriptor: SerialDescriptor = JsonObject.serializer().descriptor

    override fun serialize(encoder: Encoder, value: Map<String, Any?>) {
        val jsonEncoder = encoder as? JsonEncoder
            ?: throw SerializationException("StringAnyMapSerializer only works with JSON")
        jsonEncoder.encodeJsonElement(JsonObject(value.mapValues { (_, v) -> v.toJsonElement() }))
    }

    override fun deserialize(decoder: Decoder): Map<String, Any?> {
        val jsonDecoder = decoder as? JsonDecoder
            ?: throw SerializationException("StringAnyMapSerializer only works with JSON")
        val element = jsonDecoder.decodeJsonElement()
        val jsonObject = element as? JsonObject
            ?: throw SerializationException("Expected an object for Map<String, Any?>")
        return jsonObject.mapValues { (_, v) -> v.toDynamicValue() }
    }
}

@OptIn(ExperimentalSerializationApi::class)
object NullableStringAnyMapSerializer : KSerializer<Map<String, Any?>?> {
    override val descriptor: SerialDescriptor = StringAnyMapSerializer.descriptor

    override fun serialize(encoder: Encoder, value: Map<String, Any?>?) {
        if (value == null) {
            encoder.encodeNull()
            return
        }
        StringAnyMapSerializer.serialize(encoder, value)
    }

    override fun deserialize(decoder: Decoder): Map<String, Any?>? {
        if (!decoder.decodeNotNullMark()) {
            decoder.decodeNull()
            return null
        }
        return StringAnyMapSerializer.deserialize(decoder)
    }
}

private fun Any?.toJsonElement(): JsonElement =
    when (this) {
        null -> JsonNull
        is JsonElement -> this
        is String -> JsonPrimitive(this)
        is Boolean -> JsonPrimitive(this)
        is Int -> JsonPrimitive(this)
        is Long -> JsonPrimitive(this)
        is Float -> JsonPrimitive(this)
        is Double -> JsonPrimitive(this)
        is Number -> JsonPrimitive(this.toDouble())
        is Map<*, *> -> JsonObject(entries.associate { (k, v) -> k.toString() to v.toJsonElement() })
        is Iterable<*> -> JsonArray(map { it.toJsonElement() })
        else -> JsonPrimitive(toString())
    }

private fun JsonElement.toDynamicValue(): Any? =
    when (this) {
        JsonNull -> null
        is JsonObject -> mapValues { (_, v) -> v.toDynamicValue() }
        is JsonArray -> map { it.toDynamicValue() }
        is JsonPrimitive -> when {
            isString -> content
            booleanOrNull != null -> booleanOrNull
            longOrNull != null -> longOrNull
            doubleOrNull != null -> doubleOrNull
            else -> content
        }
    }

