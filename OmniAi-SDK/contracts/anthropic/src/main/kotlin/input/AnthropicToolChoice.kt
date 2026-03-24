package org.omniaigateway.contracts.anthropic.input

import kotlinx.serialization.Serializable

@Serializable
data class AnthropicToolChoice(
    val type: String,
    val name: String? = null,
)
