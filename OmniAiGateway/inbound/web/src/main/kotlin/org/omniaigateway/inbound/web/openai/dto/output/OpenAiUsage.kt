package org.omniaigateway.inbound.web.openai.dto.output

data class OpenAiUsage(
    val totalTokens: Int? = null,
    val completionTokens: Int? = null,
    val promptTokens: Int? = null,
)

