package org.omniai.sdk.contracts.gemini.input

import kotlinx.serialization.Serializable

@Serializable
data class GeminiContent(
    val role: String? = null,
    val parts: List<GeminiPart>
)
