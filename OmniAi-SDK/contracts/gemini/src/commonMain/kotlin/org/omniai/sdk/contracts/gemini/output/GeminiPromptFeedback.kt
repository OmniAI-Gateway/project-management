package org.omniai.sdk.contracts.gemini.output

data class GeminiPromptFeedback(
    val blockReason: String? = null,
    val safetyRatings: List<GeminiSafetyRating>? = null
)

