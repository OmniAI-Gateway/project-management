package org.omniai.sdk.contracts.openai.output

import kotlinx.serialization.Serializable

@Serializable
data class OpenAiToolCallOutput(
    val id: String? = null,
    val index: Int? = null,
    val type: String? = null,
    val function: OpenAiToolCallFunctionOutput,
)