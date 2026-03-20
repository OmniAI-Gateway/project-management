package org.omniaigateway.inbound.web.openai.dto.input

data class OpenAiToolCall(
    val id: String? = null,
    val index: Int? = null,
    val type: String = "function",
    val function: OpenAiToolCallFunction,
)
