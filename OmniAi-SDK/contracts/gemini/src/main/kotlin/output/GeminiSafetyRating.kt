package org.omniaigateway.inbound.web.gemini.dto.output

data class GeminiSafetyRating(
    val category: String? = null,
    val probability: String? = null,
    val blocked: Boolean? = null
)
