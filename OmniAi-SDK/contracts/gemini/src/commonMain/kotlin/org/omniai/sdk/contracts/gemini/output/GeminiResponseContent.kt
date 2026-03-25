package org.omniai.sdk.contracts.gemini.output

data class GeminiResponseContent(
    val parts: List<GeminiResponsePart> = emptyList(),
    val role: String? = null
)

