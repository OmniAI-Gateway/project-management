package org.omniaigateway.contracts.openai.output

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OpenAiUsage(
    @SerialName("total_tokens")
    val totalTokens: Int? = null,
    @SerialName("completion_tokens")
    val completionTokens: Int? = null,
    @SerialName("prompt_tokens")
    val promptTokens: Int? = null,
)

