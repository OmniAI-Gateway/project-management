package org.omniaigateway.inbound.web.openai.dto.output

data class OpenAiMessageOutput(
    val toolCalls: List<OpenAiToolCallOutput>? = null,
    val content: String? = null,
    val role: String,
)



