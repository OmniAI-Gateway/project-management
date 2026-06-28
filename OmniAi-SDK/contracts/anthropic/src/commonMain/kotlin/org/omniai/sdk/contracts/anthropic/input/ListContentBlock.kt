package org.omniai.sdk.contracts.anthropic.input

import kotlinx.serialization.Serializable

@Serializable
data class ListContentBlock(
    val blocks: List<AnthropicInputContentBlock>,
) : AnthropicContent
