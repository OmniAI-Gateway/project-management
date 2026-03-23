package org.omniaigateway.contracts.openai.input

import kotlinx.serialization.Serializable
import org.omniaigateway.contracts.openai.serialization.JsonAnyMapSerializer

@Serializable
data class OpenAiFunctionDefinition(
    val name: String,
    val description: String? = null,
    @Serializable(with = JsonAnyMapSerializer::class)
    val parameters: Map<String, Any?>? = null,
)
