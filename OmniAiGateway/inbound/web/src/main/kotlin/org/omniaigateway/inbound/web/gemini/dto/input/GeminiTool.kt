package org.omniaigateway.inbound.web.gemini.dto.input

data class GeminiTool(
    val functionDeclarations: List<GeminiFunctionDeclaration>? = null,
    val googleSearch: Map<String, Any?>? = null,
    val urlContext: Map<String, Any?>? = null
)

