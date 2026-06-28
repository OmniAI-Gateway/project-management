package org.omniai.sdk.contracts.gemini.output

import kotlinx.serialization.Serializable

@Serializable
data class GeminiGenerateContentResponse(
    val candidates: List<GeminiCandidate> = emptyList(),
    val usageMetadata: GeminiUsageMetadata? = null,
    val modelVersion: String? = null,
    val responseId: String? = null,
    val promptFeedback: GeminiPromptFeedback? = null,
)
