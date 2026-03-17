package org.omniaigateway.inbound.web.anthropic.dto.input

data class AnthropicMessageInput(
    val role: String,
    val content: AnthropicContent
)

