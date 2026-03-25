package org.omniai.sdk.contracts.gemini.input

data class GeminiToolConfig(
    val functionCallingConfig: GeminiFunctionCallingConfig? = null
)

