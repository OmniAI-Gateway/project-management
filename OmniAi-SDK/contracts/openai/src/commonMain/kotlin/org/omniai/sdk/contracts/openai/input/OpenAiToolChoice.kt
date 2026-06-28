package org.omniai.sdk.contracts.openai.input

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

@Serializable(with = OpenAiToolChoiceSerializer::class)
sealed interface OpenAiToolChoice {
    @Serializable
    data class Mode(
        val value: String,
    ) : OpenAiToolChoice

    @Serializable
    data class Function(
        val type: String = "function",
        val function: FunctionRef,
    ) : OpenAiToolChoice
}

@Serializable
data class FunctionRef(
    val name: String,
)

object OpenAiToolChoiceSerializer : KSerializer<OpenAiToolChoice> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("OpenAiToolChoice")

    override fun serialize(
        encoder: Encoder,
        value: OpenAiToolChoice,
    ) {
        val jsonEncoder =
            encoder as? JsonEncoder
                ?: throw SerializationException("OpenAiToolChoiceSerializer only works with JSON")
        when (value) {
            is OpenAiToolChoice.Mode -> {
                jsonEncoder.encodeJsonElement(JsonPrimitive(value.value))
            }

            is OpenAiToolChoice.Function -> {
                jsonEncoder.encodeSerializableValue(
                    OpenAiToolChoice.Function.serializer(),
                    value,
                )
            }
        }
    }

    override fun deserialize(decoder: Decoder): OpenAiToolChoice {
        val jsonDecoder =
            decoder as? JsonDecoder
                ?: throw SerializationException("OpenAiToolChoiceSerializer only works with JSON")
        return when (val element = jsonDecoder.decodeJsonElement()) {
            is JsonPrimitive -> {
                if (!element.isString) {
                    throw SerializationException("Expected tool_choice mode to be a string")
                }
                OpenAiToolChoice.Mode(element.content)
            }

            is JsonObject -> {
                jsonDecoder.json.decodeFromJsonElement(OpenAiToolChoice.Function.serializer(), element)
            }

            else -> {
                throw SerializationException("Expected tool_choice to be a string or object")
            }
        }
    }
}
