package org.omniaigateway.contracts.openai.input

data class OpenAiResponseFormat(
    val type: String,
    val jsonSchema: OpenAiJsonSchema? = null
)
