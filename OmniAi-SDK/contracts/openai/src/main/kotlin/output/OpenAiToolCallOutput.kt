package org.omniaigateway.contracts.openai.output



data class OpenAiToolCallOutput(
    val id: String,
    val index: Int? = null,
    val type: String,
    val function: OpenAiToolCallFunctionOutput,
)