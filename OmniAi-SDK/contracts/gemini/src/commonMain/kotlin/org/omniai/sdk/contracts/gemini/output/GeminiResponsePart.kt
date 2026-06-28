package org.omniai.sdk.contracts.gemini.output

import kotlinx.serialization.Serializable

@Serializable
data class GeminiResponsePart(
    val text: String? = null,
    val thoughtSignature: String? = null,
    val functionCall: GeminiFunctionCall? = null,
)
