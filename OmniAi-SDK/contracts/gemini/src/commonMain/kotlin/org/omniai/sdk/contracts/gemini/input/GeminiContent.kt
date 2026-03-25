package org.omniai.sdk.contracts.gemini.input

data class GeminiContent(
    val role: String? = null,
    val parts: List<GeminiPart>
)
