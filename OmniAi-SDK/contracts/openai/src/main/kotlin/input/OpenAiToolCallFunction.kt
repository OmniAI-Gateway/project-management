package org.omniaigateway.contracts.openai.input

data class OpenAiToolCallFunction(
    val name: String,
    val arguments: String,
)
