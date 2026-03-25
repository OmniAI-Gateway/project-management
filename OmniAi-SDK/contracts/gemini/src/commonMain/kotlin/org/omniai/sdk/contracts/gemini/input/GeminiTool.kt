package org.omniai.sdk.contracts.gemini.input

data class GeminiTool(
    val functionDeclarations: List<GeminiFunctionDeclaration>? = null,
    val googleSearch: Map<String, Any?>? = null,
    val urlContext: Map<String, Any?>? = null
)

