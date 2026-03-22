package org.omniaigateway.inbound.web.gemini.dto.output

data class GeminiFunctionCall(
    val name: String,
    val args: Map<String, Any?>? = null,
    val id: String? = null
)
