package org.omniaigateway.inbound.web.openai.dto.input

data class OpenAiResponseFormat(
    val type: String,
    val jsonSchema: OpenAiJsonSchema? = null
)
