package org.omniaigateway.inbound.web.anthropic.dto.input

data class ListContentBlock(
    val blocks: List<AnthropicInputContentBlock>
) : AnthropicContent

