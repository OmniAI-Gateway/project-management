package org.omniaigateway.contracts.gemini.input

data class GeminiThinkingConfig(
    val includeThoughts: Boolean? = null,
    val includeThoughtSignature: Boolean? = null,
    val thinkingLevel: String? = null
)
