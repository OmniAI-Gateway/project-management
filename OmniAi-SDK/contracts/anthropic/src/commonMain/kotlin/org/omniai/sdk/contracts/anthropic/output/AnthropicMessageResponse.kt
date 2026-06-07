package org.omniai.sdk.contracts.anthropic.output

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AnthropicMessageResponse(
    val id: String,
    val type: String,
    val role: String,
    val model: String,
    val content: List<AnthropicOutputContent> = emptyList(),
    @SerialName("stop_reason")
    val stopReason: AnthropicStopReason? = null,
    @SerialName("stop_sequence")
    val stopSequence: String? = null,
    val usage: AnthropicUsage? = null
)

@Serializable
enum class AnthropicStopReason {
    @SerialName("end_turn")
    END_TURN,

    @SerialName("max_tokens")
    MAX_TOKENS,

    @SerialName("stop_sequence")
    STOP_SEQUENCE,

    @SerialName("tool_use")
    TOOL_USE
}
