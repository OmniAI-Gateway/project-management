package org.omniaigateway.contracts.openai.output

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class OpenAiToolCallFunctionOutput(
    val name: String? = null,
    val arguments: JsonObject,
)