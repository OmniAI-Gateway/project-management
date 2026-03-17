package org.omniaigateway.inbound.web.anthropic.dto.output

data class AnthropicError(
    val type: String,
    val message: String,
)

