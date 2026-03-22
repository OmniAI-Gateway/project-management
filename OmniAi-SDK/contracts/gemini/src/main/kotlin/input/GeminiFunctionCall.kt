package org.omniaigateway.contracts.gemini.input

data class GeminiFunctionCall(
    val name: String,
    val args: Map<String, Any?>? = null,
    val id: String? = null
)
