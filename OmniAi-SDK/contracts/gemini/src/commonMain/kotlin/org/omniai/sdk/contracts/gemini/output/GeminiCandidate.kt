package org.omniai.sdk.contracts.gemini.output

data class GeminiCandidate(
    val content: GeminiResponseContent? = null,
    val finishReason: String? = null,
    val finishMessage: String? = null,
    val index: Int? = null
)

