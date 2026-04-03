package org.omniai.sdk.contracts.anthropic.output

import kotlinx.serialization.Serializable

@Serializable
data class AnthropicErrorResponse(
    val type: String,
    val error: AnthropicError,
)

