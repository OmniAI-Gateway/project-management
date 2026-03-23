package org.omniaigateway.contracts.openai.input

import kotlinx.serialization.Serializable
import org.omniaigateway.contracts.openai.serialization.JsonAnyMapSerializer

@Serializable
data class OpenAiJsonSchema(
    val name: String,
    val strict: Boolean? = null,
    @Serializable(with = JsonAnyMapSerializer::class)
    val schema: Map<String, Any?>,
)