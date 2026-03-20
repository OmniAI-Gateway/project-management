package org.omniaigateway.inbound.web.gemini.dto.input

data class GeminiFunctionDeclaration(
    val name: String,
    val description: String,
    val parameters: Map<String, Any?>
)
