package org.omniaigateway.contracts.anthropic.input

data class ListContentBlock(
    val blocks: List<AnthropicInputContentBlock>
) : AnthropicContent
