package org.omniai.sdk.contracts.gemini.output

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class GeminiFunctionCall(
    val name: String,
    val args: JsonObject? = null,
    val id: String? = null,
    @SerialName("thought_signature")
    val thoughtSignature: String? = null
)
