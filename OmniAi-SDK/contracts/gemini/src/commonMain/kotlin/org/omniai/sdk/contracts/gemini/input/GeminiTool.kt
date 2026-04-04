package org.omniai.sdk.contracts.gemini.input

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class GeminiTool(
    val functionDeclarations: List<GeminiFunctionDeclaration>? = null,
    val googleSearch: JsonObject? = null,
    val urlContext: JsonObject? = null
)

