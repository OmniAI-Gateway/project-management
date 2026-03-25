package org.omniai.sdk.contracts.openai.output

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OpenAiChoice(
    val index: Int,
    val message: OpenAiMessageOutput? = null,
    val delta: OpenAiDelta? = null,
    @SerialName("finish_reason")
    val finishReason: String? = null,
)
