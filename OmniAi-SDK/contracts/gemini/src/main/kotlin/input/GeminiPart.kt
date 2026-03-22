package org.omniaigateway.contracts.gemini.input

data class GeminiPart(
    val text: String? = null,
    val thoughtSignature: String? = null,
    val inlineData: GeminiInlineData? = null,
    val functionCall: GeminiFunctionCall? = null,
    val functionResponse: GeminiFunctionResponse? = null
)

