package org.omniaigateway.inbound.web.anthropic.dto.input

data class AnthropicMessagesRequest(
    val model: String,
    val maxTokens: Int,
    val messages: List<AnthropicMessageInput>,
    val system: String? = null,
    val tools: List<AnthropicToolDefinition>? = null,
    val toolChoice: AnthropicToolChoice? = null,
    val stream: Boolean? = null,
    val temperature: Double? = null,
    val topP: Double? = null,
    val topK: Int? = null,
    val stopSequences: List<String>? = null,
    val stopToken: String? = null,
    val thinking: AnthropicThinkingConfig? = null,
    val metadata: Map<String, Any?>? = null
)

