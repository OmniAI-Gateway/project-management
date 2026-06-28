package org.omniai.sdk.contracts.openai.input

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class OpenAiJsonSchema(
    val name: String,
    val strict: Boolean? = null,
    val schema: JsonObject,
)
