package org.omniai.sdk.contracts.openai.output

import kotlinx.serialization.Serializable

@Serializable
data class OpenAiErrorResponse(
    val error: OpenAiError,
)
