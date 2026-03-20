package org.omniaigateway.inbound.web.gemini.dto.input

data class GeminiThinkingConfig(
    val includeThoughts: Boolean? = null,
    val includeThoughtSignature: Boolean? = null,
    val thinkingLevel: String? = null
)
