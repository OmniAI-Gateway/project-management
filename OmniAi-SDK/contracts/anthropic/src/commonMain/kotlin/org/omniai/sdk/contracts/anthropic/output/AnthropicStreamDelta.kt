package org.omniai.sdk.contracts.anthropic.output

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.omniai.sdk.contracts.anthropic.serialization.AnthropicStreamDeltaSerializer

@Serializable(with = AnthropicStreamDeltaSerializer::class)
sealed interface AnthropicStreamDelta {
    val type: String

    @Serializable
    data class TextDelta(
        val text: String,
        override val type: String = "text_delta"
    ) : AnthropicStreamDelta

    @Serializable
    data class ThinkingDelta(
        val thinking: String,
        override val type: String = "thinking_delta"
    ) : AnthropicStreamDelta

    @Serializable
    data class SignatureDelta(
        val signature: String,
        override val type: String = "signature_delta"
    ) : AnthropicStreamDelta

    @Serializable
    data class InputJsonDelta(
        @SerialName("partial_json")
        val partialJson: String,
        override val type: String = "input_json_delta"
    ) : AnthropicStreamDelta
}
