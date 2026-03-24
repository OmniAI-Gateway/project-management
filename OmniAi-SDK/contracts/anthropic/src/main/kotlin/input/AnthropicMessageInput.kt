package org.omniaigateway.contracts.anthropic.input

import kotlinx.serialization.Serializable

@Serializable
data class AnthropicMessageInput(
    val role: String,
    val content: AnthropicContent
)
