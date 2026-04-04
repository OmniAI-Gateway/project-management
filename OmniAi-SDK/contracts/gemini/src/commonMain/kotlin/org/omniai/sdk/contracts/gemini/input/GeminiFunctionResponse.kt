package org.omniai.sdk.contracts.gemini.input

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class GeminiFunctionResponse(
    val name: String,
    val response: JsonObject
)
