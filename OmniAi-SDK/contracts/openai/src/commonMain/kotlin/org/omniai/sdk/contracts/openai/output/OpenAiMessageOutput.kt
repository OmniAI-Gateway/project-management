package org.omniai.sdk.contracts.openai.output

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OpenAiMessageOutput(
    @SerialName("tool_calls")
    val toolCalls: List<OpenAiToolCallOutput>? = null,
    val content: String? = null,
    val role: String,
)



