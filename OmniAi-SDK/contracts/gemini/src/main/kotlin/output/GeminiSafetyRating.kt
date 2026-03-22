package org.omniaigateway.contracts.gemini.output

data class GeminiSafetyRating(
    val category: String? = null,
    val probability: String? = null,
    val blocked: Boolean? = null
)
