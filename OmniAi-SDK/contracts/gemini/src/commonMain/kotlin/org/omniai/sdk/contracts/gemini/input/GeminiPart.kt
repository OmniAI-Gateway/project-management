package org.omniai.sdk.contracts.gemini.input

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GeminiPart(
    val text: String? = null,
    val inlineData: GeminiInlineData? = null,
    @SerialName("thought_signature")
    val thoughtSignature: String? = "skip_thought_signature_validator",
    val functionCall: GeminiFunctionCall? = null,
    val functionResponse: GeminiFunctionResponse? = null,
)
