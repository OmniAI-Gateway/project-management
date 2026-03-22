package org.omniaigateway.contracts.openai.output


data class OpenAiDelta(
    val role: String? = null,
    val content: String? = null,
    val toolCalls: List<OpenAiToolCallOutput>? = null,
)
