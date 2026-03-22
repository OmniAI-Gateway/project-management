package org.omniaigateway.contracts.gemini.input

data class GeminiFunctionDeclaration(
    val name: String,
    val description: String,
    val parameters: Map<String, Any?>
)
