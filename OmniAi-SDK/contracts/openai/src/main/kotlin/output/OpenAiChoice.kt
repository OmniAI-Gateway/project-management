package org.omniaigateway.contracts.openai.output

data class OpenAiChoice(
    val index: Int,
    val message: OpenAiMessageOutput? = null,
    val delta: OpenAiDelta? = null,
    val finishReason: String? = null,
)
