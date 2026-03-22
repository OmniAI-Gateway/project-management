package org.omniaigateway.inbound.web.openai.dto.output

data class OpenAiToolCallFunctionOutput(
    val name: String,
    val arguments: String,
)