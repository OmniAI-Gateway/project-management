package org.omniai.sdk.contracts.gemini.input

data class GeminiFunctionResponse(
    val name: String,
    val response: Map<String, Any?>
)
