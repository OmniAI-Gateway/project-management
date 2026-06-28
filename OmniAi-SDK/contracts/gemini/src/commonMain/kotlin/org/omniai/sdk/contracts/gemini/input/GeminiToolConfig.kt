package org.omniai.sdk.contracts.gemini.input

import kotlinx.serialization.Serializable

@Serializable
data class GeminiToolConfig(
    val functionCallingConfig: GeminiFunctionCallingConfig? = null,
)
