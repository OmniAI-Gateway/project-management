package org.omniai.sdk.contracts.gemini.input

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class GeminiFunctionDeclaration(
    val name: String,
    val description: String,
    val parameters: JsonObject
)
