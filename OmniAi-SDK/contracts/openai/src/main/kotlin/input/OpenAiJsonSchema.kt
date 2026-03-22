package org.omniaigateway.contracts.openai.input

data class OpenAiJsonSchema(
    val name: String,
    val strict: Boolean? = null,
    val schema: Map<String, Any?>,
)