package org.omniaigateway.contracts.openai.input

data class OpenAiFunctionDefinition(
    val name: String,
    val description: String? = null,
    val parameters: Map<String, Any?>? = null,
)
