package org.omniaigateway.contracts.gemini.output

data class GeminiGenerateContentResponse(
    val candidates: List<GeminiCandidate> = emptyList(),
    val usageMetadata: GeminiUsageMetadata? = null,
    val modelVersion: String? = null,
    val responseId: String? = null,
    val promptFeedback: GeminiPromptFeedback? = null
)

