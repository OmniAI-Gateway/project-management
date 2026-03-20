package org.omniaigateway.inbound.web.gemini.dto.input

data class GeminiContent(
    val role: String? = null,
    val parts: List<GeminiPart>
)
