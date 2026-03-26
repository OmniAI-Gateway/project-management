package org.omniai.sdk.contracts.gemini.output

import kotlinx.serialization.Serializable

@Serializable
data class GeminiTokenDetail(
    val modality: String,
    val tokenCount: Int
)

