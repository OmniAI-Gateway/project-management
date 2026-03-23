package org.omniaigateway.contracts.openai.input

import kotlinx.serialization.Serializable

@Serializable
data class OpenAiToolCallFunction(
    val name: String,
    val arguments: String,
)
