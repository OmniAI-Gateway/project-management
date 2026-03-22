package org.omniaigateway.contracts.gemini.output

data class GeminiResponseContent(
    val parts: List<GeminiResponsePart> = emptyList(),
    val role: String? = null
)

