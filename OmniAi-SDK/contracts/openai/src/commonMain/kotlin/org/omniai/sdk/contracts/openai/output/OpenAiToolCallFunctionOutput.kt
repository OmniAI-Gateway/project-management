package org.omniai.sdk.contracts.openai.output

import kotlinx.serialization.Serializable

@Serializable
data class OpenAiToolCallFunctionOutput(
    val name: String? = null,
    val arguments: String,
)
