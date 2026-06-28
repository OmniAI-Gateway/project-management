package org.omniai.sdk.contracts.anthropic.input

import kotlinx.serialization.Serializable

@Serializable
data class RawText(
    val text: String,
) : AnthropicContent
