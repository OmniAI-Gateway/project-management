package org.omniaigateway.inbound.web.openai.dto.input

data class OpenAiMessageInput(
    val role: String,
    val content: String? = null,
    val toolCalls: List<OpenAiToolCall>? = null,
    val toolCallId: String? = null,
)
