package org.omniai.sdk.contracts.anthropic.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
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
import org.omniai.sdk.contracts.anthropic.input.AnthropicContent
import org.omniai.sdk.contracts.anthropic.input.AnthropicInputContentBlock
import org.omniai.sdk.contracts.anthropic.input.ListContentBlock
import org.omniai.sdk.contracts.anthropic.input.RawText
import org.omniai.sdk.contracts.anthropic.output.AnthropicOutputContent
import org.omniai.sdk.contracts.anthropic.output.AnthropicStreamDelta
import org.omniai.sdk.contracts.anthropic.output.AnthropicStreamEvent

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
        val obj = element.jsonObject
        val explicitType = obj["type"]?.jsonPrimitive?.content

        val inferredType = explicitType ?: when {
            obj["name"] != null && (obj["input"] != null || obj["id"] != null) -> "tool_use"
            obj["tool_use_id"] != null -> "tool_result"
            obj["text"] != null -> "text"
            obj["budget_tokens"] != null -> "thinking"
            else -> null
        }

        return when (inferredType) {
            "text" -> AnthropicInputContentBlock.Text.serializer()
            "tool_use" -> AnthropicInputContentBlock.ToolUse.serializer()
            "tool_result" -> AnthropicInputContentBlock.ToolResult.serializer()
            "thinking" -> AnthropicInputContentBlock.Thinking.serializer()
            else -> throw SerializationException("Unknown Anthropic input content block type: $explicitType")
        }
    }

}

object AnthropicOutputContentSerializer :
    JsonContentPolymorphicSerializer<AnthropicOutputContent>(AnthropicOutputContent::class) {
    override fun selectDeserializer(element: JsonElement): KSerializer<out AnthropicOutputContent> {
        return when (val type = element.jsonObject["type"]?.jsonPrimitive?.content) {
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
        return when (val type = element.jsonObject["type"]?.jsonPrimitive?.content) {
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
        return when (val type = element.jsonObject["type"]?.jsonPrimitive?.content) {
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

