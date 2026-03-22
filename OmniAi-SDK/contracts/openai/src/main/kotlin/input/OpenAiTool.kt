package org.omniaigateway.contracts.openai.input

data class OpenAiTool(
    val type: String = "function",
    val function: OpenAiFunctionDefinition,
)
