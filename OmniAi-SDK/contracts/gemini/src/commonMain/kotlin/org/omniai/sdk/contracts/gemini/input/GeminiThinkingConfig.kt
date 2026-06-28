package org.omniai.sdk.contracts.gemini.input

import kotlinx.serialization.Serializable

@Serializable
data class GeminiThinkingConfig(
    val includeThoughts: Boolean? = null,
    val includeThoughtSignature: Boolean? = null,
    val thinkingLevel: String? = null,
)
