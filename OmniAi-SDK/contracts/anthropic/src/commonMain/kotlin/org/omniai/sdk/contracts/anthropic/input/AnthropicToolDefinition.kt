package org.omniai.sdk.contracts.anthropic.input

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class AnthropicToolDefinition(
    val name: String,
    val description: String,
    @SerialName("input_schema")
    val inputSchema: JsonObject,
)
