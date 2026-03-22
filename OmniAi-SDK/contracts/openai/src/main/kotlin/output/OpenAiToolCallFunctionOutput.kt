package org.omniaigateway.contracts.openai.output

data class OpenAiToolCallFunctionOutput(
    val name: String,
    val arguments: Map<String, Any?>,
)