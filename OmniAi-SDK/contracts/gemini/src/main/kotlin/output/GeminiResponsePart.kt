package org.omniaigateway.contracts.gemini.output

data class GeminiResponsePart(
    val text: String? = null,
    val thoughtSignature: String? = null,
    val functionCall: GeminiFunctionCall? = null
)

