package org.omniaigateway.inbound.web.gemini.dto.output

data class GeminiResponseContent(
    val parts: List<GeminiResponsePart> = emptyList(),
    val role: String? = null
)

