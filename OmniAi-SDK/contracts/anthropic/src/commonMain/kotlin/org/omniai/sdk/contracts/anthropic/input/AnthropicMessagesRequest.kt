package org.omniai.sdk.contracts.anthropic.input

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class AnthropicMessagesRequest(
    val model: String,
    @SerialName("max_tokens")
    val maxTokens: Int,
    val messages: List<AnthropicMessageInput>,
    val system: AnthropicContent? = null,
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
    @SerialName("output_config")
    val outputConfig: AnthropicOutputConfig? = null,
    val thinking: AnthropicThinkingConfig? = null,
    val metadata: JsonElement? = null
)
