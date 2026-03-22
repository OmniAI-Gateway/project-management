package org.omniaigateway.contracts.openai.output

data class OpenAiUsage(
    val totalTokens: Int? = null,
    val completionTokens: Int? = null,
    val promptTokens: Int? = null,
)

