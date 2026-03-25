package org.omniai.sdk.contracts.openai.output

import kotlinx.serialization.Serializable

@Serializable
data class OpenAiToolCallOutput(
    val id: String,
    val index: Int? = null,
    val type: String,
    val function: OpenAiToolCallFunctionOutput,
)