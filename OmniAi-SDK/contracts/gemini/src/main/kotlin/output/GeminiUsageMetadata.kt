package org.omniaigateway.inbound.web.gemini.dto.output

data class GeminiUsageMetadata(
    val promptTokenCount: Int? = null,
    val candidatesTokenCount: Int? = null,
    val totalTokenCount: Int? = null,
    val thoughtsTokenCount: Int? = null,
    val promptTokensDetails: List<GeminiTokenDetail>? = null,
    val candidatesTokensDetails: List<GeminiTokenDetail>? = null
)

