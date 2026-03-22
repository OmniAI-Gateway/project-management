package org.omniaigateway.contracts.anthropic.output

sealed interface AnthropicStreamDelta {
    val type: String

    data class TextDelta(
        val text: String,
        override val type: String = "text_delta"
    ) : AnthropicStreamDelta

    data class ThinkingDelta(
        val thinking: String,
        override val type: String = "thinking_delta"
    ) : AnthropicStreamDelta

    data class SignatureDelta(
        val signature: String,
        override val type: String = "signature_delta"
    ) : AnthropicStreamDelta

    data class InputJsonDelta(
        val partialJson: String,
        override val type: String = "input_json_delta"
    ) : AnthropicStreamDelta
}
