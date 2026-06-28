package org.omniai.sdk.contracts.openai.input

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonPrimitive

@Serializable(with = OpenAiStopSerializer::class)
sealed interface OpenAiStop {
    @Serializable
    data class Single(
        val value: String,
    ) : OpenAiStop

    @Serializable
    data class Multiple(
        val values: List<String>,
    ) : OpenAiStop
}

object OpenAiStopSerializer : KSerializer<OpenAiStop> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("OpenAiStop")

    override fun serialize(
        encoder: Encoder,
        value: OpenAiStop,
    ) {
        val jsonEncoder =
            encoder as? JsonEncoder
                ?: throw SerializationException("OpenAiStopSerializer only works with JSON")
        when (value) {
            is OpenAiStop.Single -> jsonEncoder.encodeJsonElement(JsonPrimitive(value.value))
            is OpenAiStop.Multiple -> jsonEncoder.encodeJsonElement(JsonArray(value.values.map(::JsonPrimitive)))
        }
    }

    override fun deserialize(decoder: Decoder): OpenAiStop {
        val jsonDecoder =
            decoder as? JsonDecoder
                ?: throw SerializationException("OpenAiStopSerializer only works with JSON")
        return when (val element = jsonDecoder.decodeJsonElement()) {
            is JsonPrimitive -> {
                if (!element.isString) {
                    throw SerializationException("Expected stop to be a string")
                }
                OpenAiStop.Single(element.content)
            }

            is JsonArray -> {
                OpenAiStop.Multiple(
                    element.map { jsonElement ->
                        val primitive =
                            jsonElement as? JsonPrimitive
                                ?: throw SerializationException("Expected stop array items to be strings")
                        if (!primitive.isString) {
                            throw SerializationException("Expected stop array items to be strings")
                        }
                        primitive.content
                    },
                )
            }

            else -> {
                throw SerializationException("Expected stop to be a string or an array of strings")
            }
        }
    }
}
