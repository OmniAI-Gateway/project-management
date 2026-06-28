package org.omniai.sdk.contracts.gemini.input

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class GeminiFunctionCall(
    val name: String,
    val args: JsonObject? = null,
)
