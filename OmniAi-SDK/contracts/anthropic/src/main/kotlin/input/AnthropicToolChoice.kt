package org.omniaigateway.contracts.anthropic.input

data class AnthropicToolChoice(
    val type: String,
    val name: String? = null,
)
