package org.omniai.sdk.contracts.gemini.output

import kotlinx.serialization.Serializable

@Serializable
data class GeminiPromptFeedback(
    val blockReason: String? = null,
    val safetyRatings: List<GeminiSafetyRating>? = null,
)
