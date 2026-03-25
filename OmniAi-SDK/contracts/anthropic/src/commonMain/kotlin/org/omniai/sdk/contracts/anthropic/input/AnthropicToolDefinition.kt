package org.omniai.sdk.contracts.anthropic.input

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.omniai.sdk.contracts.anthropic.serialization.StringAnyMapSerializer

@Serializable
data class AnthropicToolDefinition(
    val name: String,
    val description: String,
    @SerialName("input_schema")
    @Serializable(with = StringAnyMapSerializer::class)
    val inputSchema: Map<String, Any?>,
)
