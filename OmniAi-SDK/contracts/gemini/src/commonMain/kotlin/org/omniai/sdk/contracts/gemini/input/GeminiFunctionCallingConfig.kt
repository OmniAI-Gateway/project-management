package org.omniai.sdk.contracts.gemini.input

import kotlinx.serialization.Serializable

@Serializable
data class GeminiFunctionCallingConfig(
    val mode: String,
    val allowedFunctionNames: List<String>? = null,
)
