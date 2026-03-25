package org.omniai.sdk.contracts.gemini.output

data class GeminiResponsePart(
    val text: String? = null,
    val thoughtSignature: String? = null,
    val functionCall: GeminiFunctionCall? = null
)

