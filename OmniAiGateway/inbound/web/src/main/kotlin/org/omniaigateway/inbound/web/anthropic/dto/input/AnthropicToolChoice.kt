package org.omniaigateway.inbound.web.anthropic.dto.input

data class AnthropicToolChoice(
    val type: String,
    val name: String? = null,
)

