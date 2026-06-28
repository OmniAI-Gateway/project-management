package org.omniai.sdk.contracts.gemini.output

import kotlinx.serialization.Serializable

@Serializable
data class GeminiResponseContent(
    val parts: List<GeminiResponsePart> = emptyList(),
    val role: String? = null,
)
