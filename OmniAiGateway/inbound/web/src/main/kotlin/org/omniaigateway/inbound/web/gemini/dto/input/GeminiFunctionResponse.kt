package org.omniaigateway.inbound.web.gemini.dto.input

data class GeminiFunctionResponse(
    val name: String,
    val response: Map<String, Any?>
)
