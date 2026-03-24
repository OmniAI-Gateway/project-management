package org.omniaigateway.contracts.anthropic.input

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.omniaigateway.contracts.anthropic.serialization.NullableStringAnyMapSerializer

@Serializable
data class AnthropicMessagesRequest(
    val model: String,
    @SerialName("max_tokens")
    val maxTokens: Int,
    val messages: List<AnthropicMessageInput>,
    val system: String? = null,
    val tools: List<AnthropicToolDefinition>? = null,
    @SerialName("tool_choice")
    val toolChoice: AnthropicToolChoice? = null,
    val stream: Boolean? = null,
    val temperature: Double? = null,
    @SerialName("top_p")
    val topP: Double? = null,
    @SerialName("top_k")
    val topK: Int? = null,
    @SerialName("stop_sequences")
    val stopSequences: List<String>? = null,
    @SerialName("stop_token")
    val stopToken: String? = null,
    val thinking: AnthropicThinkingConfig? = null,
    @Serializable(with = NullableStringAnyMapSerializer::class)
    val metadata: Map<String, Any?>? = null
)
