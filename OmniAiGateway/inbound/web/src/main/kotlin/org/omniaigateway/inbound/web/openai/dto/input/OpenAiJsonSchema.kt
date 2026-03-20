package org.omniaigateway.inbound.web.openai.dto.input

data class OpenAiJsonSchema(
    val name: String,
    val strict: Boolean? = null,
    val schema: Map<String, Any?>,
)