package org.omniaigateway.contracts.openai.input

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class OpenAiFunctionDefinition(
    val name: String,
    val description: String? = null,
    val parameters: JsonObject? = null,
)
