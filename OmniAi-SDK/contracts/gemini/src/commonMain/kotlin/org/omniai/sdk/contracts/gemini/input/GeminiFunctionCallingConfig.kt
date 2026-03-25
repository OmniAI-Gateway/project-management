package org.omniai.sdk.contracts.gemini.input

data class GeminiFunctionCallingConfig(
    val mode: String,
    val allowedFunctionNames: List<String>? = null
)
