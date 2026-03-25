package org.omniai.sdk.contracts.gemini.output

import kotlinx.serialization.Serializable

@Serializable
data class GeminiSafetyRating(
    val category: String? = null,
    val probability: String? = null,
    val blocked: Boolean? = null
)
