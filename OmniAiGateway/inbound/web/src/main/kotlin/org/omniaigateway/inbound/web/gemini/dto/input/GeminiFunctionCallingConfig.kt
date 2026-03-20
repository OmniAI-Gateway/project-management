package org.omniaigateway.inbound.web.gemini.dto.input

data class GeminiFunctionCallingConfig(
    val mode: String,
    val allowedFunctionNames: List<String>? = null
)
