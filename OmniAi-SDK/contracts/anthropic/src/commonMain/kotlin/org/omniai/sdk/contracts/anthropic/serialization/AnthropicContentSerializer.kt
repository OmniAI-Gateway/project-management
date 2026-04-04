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
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonPrimitive
import org.omniai.sdk.contracts.anthropic.input.AnthropicContent
import org.omniai.sdk.contracts.anthropic.input.AnthropicInputContentBlock
import org.omniai.sdk.contracts.anthropic.input.ListContentBlock
import org.omniai.sdk.contracts.anthropic.input.RawText

object AnthropicContentSerializer : KSerializer<AnthropicContent> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("AnthropicContent", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: AnthropicContent) {

        val jsonEncoder = encoder as? JsonEncoder ?: throw SerializationException("AnthropicContentSerializer only works with JSON")

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
        val jsonDecoder = decoder as? JsonDecoder ?: throw SerializationException("AnthropicContentSerializer only works with JSON")
        return when (val element = jsonDecoder.decodeJsonElement()) {
            is JsonPrimitive -> RawText(element.content)
            is JsonArray -> ListContentBlock(
                blocks = jsonDecoder.json.decodeFromJsonElement(ListSerializer(AnthropicInputContentBlock.serializer()), element)
            )
            else -> throw SerializationException("Expected content to be a string or an array")
        }
    }
}


