package org.omniaigateway.inbound.web.openai.dto.input

data class OpenAiToolCallFunction(
    val name: String,
    val arguments: String,
)
