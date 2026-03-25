package org.omniai.sdk.contracts.gemini.output

import kotlinx.serialization.Serializable

@Serializable
data class GeminiCandidate(
    val content: GeminiResponseContent? = null,
    val finishReason: String? = null,
    val finishMessage: String? = null,
    val index: Int? = null
)

