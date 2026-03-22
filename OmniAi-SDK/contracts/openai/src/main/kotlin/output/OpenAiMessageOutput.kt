package org.omniaigateway.contracts.openai.output

data class OpenAiMessageOutput(
    val toolCalls: List<OpenAiToolCallOutput>? = null,
    val content: String? = null,
    val role: String,
)



