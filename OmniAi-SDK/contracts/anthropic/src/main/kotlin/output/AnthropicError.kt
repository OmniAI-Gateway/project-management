package org.omniaigateway.contracts.anthropic.output

data class AnthropicError(
    val type: String,
    val message: String,
)
