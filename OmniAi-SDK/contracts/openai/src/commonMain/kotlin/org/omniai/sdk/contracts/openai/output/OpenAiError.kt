package org.omniai.sdk.contracts.openai.output

import kotlinx.serialization.Serializable

@Serializable
data class OpenAiError(
    val message: String,
    val type: String,
    val param: String? = null,
    val code: String? = null,
)
