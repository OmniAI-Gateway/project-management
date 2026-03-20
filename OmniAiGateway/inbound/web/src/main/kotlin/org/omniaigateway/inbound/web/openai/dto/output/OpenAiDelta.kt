package org.omniaigateway.inbound.web.openai.dto.output


data class OpenAiDelta(
    val role: String? = null,
    val content: String? = null,
    val toolCalls: List<OpenAiToolCallOutput>? = null,
)
