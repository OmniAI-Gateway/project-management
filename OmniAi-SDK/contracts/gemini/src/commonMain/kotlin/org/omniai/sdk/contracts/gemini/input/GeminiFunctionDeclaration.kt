package org.omniai.sdk.contracts.gemini.input

data class GeminiFunctionDeclaration(
    val name: String,
    val description: String,
    val parameters: Map<String, Any?>
)
