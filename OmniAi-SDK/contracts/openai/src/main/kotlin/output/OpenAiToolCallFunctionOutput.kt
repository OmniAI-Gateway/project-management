package org.omniaigateway.contracts.openai.output

import kotlinx.serialization.Serializable
import org.omniaigateway.contracts.openai.serialization.JsonAnyMapSerializer

@Serializable
data class OpenAiToolCallFunctionOutput(
    val name: String? = null,
    @Serializable(with = JsonAnyMapSerializer::class)
    val arguments: Map<String, Any?>,
)