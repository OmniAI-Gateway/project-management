package org.omniaigateway.contracts.anthropic.input

data class AnthropicMessageInput(
    val role: String,
    val content: AnthropicContent
)
