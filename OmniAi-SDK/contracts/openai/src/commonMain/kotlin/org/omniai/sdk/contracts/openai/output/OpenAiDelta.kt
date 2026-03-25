package org.omniai.sdk.contracts.openai.output

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class OpenAiDelta(
    val role: String? = null,
    val content: String? = null,
    @SerialName("tool_calls")
    val toolCalls: List<OpenAiToolCallOutput>? = null,
)
