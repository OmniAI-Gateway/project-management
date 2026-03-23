package org.omniaigateway.contracts.openai.input

import kotlinx.serialization.Serializable

@Serializable
data class OpenAiToolCall(
    val id: String? = null,
    val index: Int? = null,
    val type: String = "function",
    val function: OpenAiToolCallFunction,
)
