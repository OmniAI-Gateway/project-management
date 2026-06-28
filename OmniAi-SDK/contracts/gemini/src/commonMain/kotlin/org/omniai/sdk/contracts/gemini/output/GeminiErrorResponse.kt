package org.omniai.sdk.contracts.gemini.output

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GeminiErrorResponse(
    val error: GeminiError,
)

@Serializable
data class GeminiError(
    val code: Int,
    val message: String,
    val status: String,
    val details: List<GeminiErrorDetail>,
)

@Serializable
data class GeminiErrorDetail(
    @SerialName("@type")
    val type: String,
    val reason: String,
    val domain: String,
    val metadata: GeminiErrorMetadata,
)

@Serializable
data class GeminiErrorMetadata(
    val service: String,
)
