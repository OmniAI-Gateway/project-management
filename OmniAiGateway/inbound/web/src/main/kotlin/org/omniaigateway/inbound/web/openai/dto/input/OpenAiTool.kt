package org.omniaigateway.inbound.web.openai.dto.input

data class OpenAiTool(
    val type: String = "function",
    val function: OpenAiFunctionDefinition,
)
