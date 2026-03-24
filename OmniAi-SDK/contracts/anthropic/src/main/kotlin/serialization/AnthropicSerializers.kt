package org.omniaigateway.contracts.anthropic.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import org.omniaigateway.contracts.anthropic.input.AnthropicContent
import org.omniaigateway.contracts.anthropic.input.AnthropicInputContentBlock
import org.omniaigateway.contracts.anthropic.input.ListContentBlock
import org.omniaigateway.contracts.anthropic.input.RawText
import org.omniaigateway.contracts.anthropic.output.AnthropicOutputContent
import org.omniaigateway.contracts.anthropic.output.AnthropicStreamDelta
import org.omniaigateway.contracts.anthropic.output.AnthropicStreamEvent

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

object AnthropicContentSerializer : KSerializer<AnthropicContent> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("AnthropicContent", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: AnthropicContent) {
        val jsonEncoder = encoder as? JsonEncoder
            ?: throw SerializationException("AnthropicContentSerializer only works with JSON")

        val element = when (value) {
            is RawText -> JsonPrimitive(value.text)
            is ListContentBlock -> jsonEncoder.json.encodeToJsonElement(
                ListSerializer(AnthropicInputContentBlock.serializer()),
                value.blocks
            )
        }

        jsonEncoder.encodeJsonElement(element)
    }

    override fun deserialize(decoder: Decoder): AnthropicContent {
        val jsonDecoder = decoder as? JsonDecoder
            ?: throw SerializationException("AnthropicContentSerializer only works with JSON")
        return when (val element = jsonDecoder.decodeJsonElement()) {
            is JsonPrimitive -> RawText(element.content)
            is JsonArray -> ListContentBlock(
                blocks = jsonDecoder.json.decodeFromJsonElement(ListSerializer(AnthropicInputContentBlock.serializer()), element)
            )
            else -> throw SerializationException("Expected content to be a string or an array")
        }
    }
}

object AnthropicInputContentBlockSerializer :
    JsonContentPolymorphicSerializer<AnthropicInputContentBlock>(AnthropicInputContentBlock::class) {
    override fun selectDeserializer(element: JsonElement): KSerializer<out AnthropicInputContentBlock> {
        val type = element.jsonObject["type"]?.jsonPrimitive?.content
        return when (type) {
            "text" -> AnthropicInputContentBlock.Text.serializer()
            "tool_use" -> AnthropicInputContentBlock.ToolUse.serializer()
            "tool_result" -> AnthropicInputContentBlock.ToolResult.serializer()
            "thinking" -> AnthropicInputContentBlock.Thinking.serializer()
            else -> throw SerializationException("Unknown Anthropic input content block type: $type")
        }
    }
}

object AnthropicOutputContentSerializer :
    JsonContentPolymorphicSerializer<AnthropicOutputContent>(AnthropicOutputContent::class) {
    override fun selectDeserializer(element: JsonElement): KSerializer<out AnthropicOutputContent> {
        val type = element.jsonObject["type"]?.jsonPrimitive?.content
        return when (type) {
            "text" -> AnthropicOutputContent.Text.serializer()
            "thinking" -> AnthropicOutputContent.Thinking.serializer()
            "tool_use" -> AnthropicOutputContent.ToolUse.serializer()
            else -> throw SerializationException("Unknown Anthropic output content type: $type")
        }
    }
}

object AnthropicStreamDeltaSerializer :
    JsonContentPolymorphicSerializer<AnthropicStreamDelta>(AnthropicStreamDelta::class) {
    override fun selectDeserializer(element: JsonElement): KSerializer<out AnthropicStreamDelta> {
        val type = element.jsonObject["type"]?.jsonPrimitive?.content
        return when (type) {
            "text_delta" -> AnthropicStreamDelta.TextDelta.serializer()
            "thinking_delta" -> AnthropicStreamDelta.ThinkingDelta.serializer()
            "signature_delta" -> AnthropicStreamDelta.SignatureDelta.serializer()
            "input_json_delta" -> AnthropicStreamDelta.InputJsonDelta.serializer()
            else -> throw SerializationException("Unknown Anthropic stream delta type: $type")
        }
    }
}

object AnthropicStreamEventSerializer :
    JsonContentPolymorphicSerializer<AnthropicStreamEvent>(AnthropicStreamEvent::class) {
    override fun selectDeserializer(element: JsonElement): KSerializer<out AnthropicStreamEvent> {
        val type = element.jsonObject["type"]?.jsonPrimitive?.content
        return when (type) {
            "message_start" -> AnthropicStreamEvent.MessageStart.serializer()
            "content_block_start" -> AnthropicStreamEvent.ContentBlockStart.serializer()
            "content_block_delta" -> AnthropicStreamEvent.ContentBlockDelta.serializer()
            "content_block_stop" -> AnthropicStreamEvent.ContentBlockStop.serializer()
            "message_delta" -> AnthropicStreamEvent.MessageDelta.serializer()
            "message_stop" -> AnthropicStreamEvent.MessageStop.serializer()
            "ping" -> AnthropicStreamEvent.Ping.serializer()
            "error" -> AnthropicStreamEvent.Error.serializer()
            else -> throw SerializationException("Unknown Anthropic stream event type: $type")
        }
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

