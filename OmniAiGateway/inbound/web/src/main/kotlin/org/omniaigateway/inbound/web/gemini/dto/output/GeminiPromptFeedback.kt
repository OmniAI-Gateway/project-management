package org.omniaigateway.inbound.web.gemini.dto.output

data class GeminiPromptFeedback(
    val blockReason: String? = null,
    val safetyRatings: List<GeminiSafetyRating>? = null
)

