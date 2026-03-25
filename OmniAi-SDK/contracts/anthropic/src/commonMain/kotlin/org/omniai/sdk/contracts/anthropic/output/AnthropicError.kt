package org.omniai.sdk.contracts.anthropic.output

import kotlinx.serialization.Serializable

@Serializable
data class AnthropicError(
    val type: String,
    val message: String,
)
