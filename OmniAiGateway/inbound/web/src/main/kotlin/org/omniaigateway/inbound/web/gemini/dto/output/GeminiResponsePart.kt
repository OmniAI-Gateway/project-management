package org.omniaigateway.inbound.web.gemini.dto.output

data class GeminiResponsePart(
    val text: String? = null,
    val thoughtSignature: String? = null,
    val functionCall: GeminiFunctionCall? = null
)

