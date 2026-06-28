package org.omniai.sdk.contracts.anthropic.output

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

@Serializable
@OptIn(ExperimentalSerializationApi::class)
@JsonClassDiscriminator("type")
sealed interface AnthropicStreamDelta {
    @Serializable
    @SerialName("text_delta")
    data class TextDelta(
        val text: String,
    ) : AnthropicStreamDelta

    @Serializable
    @SerialName("thinking_delta")
    data class ThinkingDelta(
        val thinking: String,
    ) : AnthropicStreamDelta

    @Serializable
    @SerialName("signature_delta")
    data class SignatureDelta(
        val signature: String,
    ) : AnthropicStreamDelta

    @Serializable
    @SerialName("input_json_delta")
    data class InputJsonDelta(
        @SerialName("partial_json")
        val partialJson: String,
    ) : AnthropicStreamDelta
}
