package org.omniai.sdk.contracts.gemini.input

import kotlinx.serialization.Serializable

@Serializable
data class GeminiInlineData(
    val mimeType: String,
    val data: String
)
