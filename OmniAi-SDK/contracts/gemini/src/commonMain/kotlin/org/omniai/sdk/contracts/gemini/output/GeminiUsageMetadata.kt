package org.omniai.sdk.contracts.gemini.output

import kotlinx.serialization.Serializable

@Serializable
data class GeminiUsageMetadata(
    val promptTokenCount: Int? = null,
    val candidatesTokenCount: Int? = null,
    val totalTokenCount: Int? = null,
    val thoughtsTokenCount: Int? = null,
    val promptTokensDetails: List<GeminiTokenDetail>? = null,
    val candidatesTokensDetails: List<GeminiTokenDetail>? = null,
)
