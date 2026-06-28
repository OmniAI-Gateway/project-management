package org.omniai.sdk.contracts.anthropic.input

import kotlinx.serialization.Serializable

@Serializable
data class AnthropicOutputConfig(
    val format: AnthropicOutputFormat? = null,
)
