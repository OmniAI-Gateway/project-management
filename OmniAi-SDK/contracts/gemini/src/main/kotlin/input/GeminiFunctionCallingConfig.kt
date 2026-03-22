package org.omniaigateway.contracts.gemini.input

data class GeminiFunctionCallingConfig(
    val mode: String,
    val allowedFunctionNames: List<String>? = null
)
