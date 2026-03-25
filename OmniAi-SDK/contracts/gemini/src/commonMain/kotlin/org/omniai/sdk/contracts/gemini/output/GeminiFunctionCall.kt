package org.omniai.sdk.contracts.gemini.output

data class GeminiFunctionCall(
    val name: String,
    val args: Map<String, Any?>? = null,
    val id: String? = null
)
