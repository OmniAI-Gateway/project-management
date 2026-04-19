package org.omniai.sdk.contracts.gemini.input

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class GeminiFunctionCall(
    val name: String,
    val args: JsonObject? = null,
    val id: String? = null,
    @SerialName("thought_signature")
    val thoughtSignature: String = "skip_thought_signature_validator"
)
