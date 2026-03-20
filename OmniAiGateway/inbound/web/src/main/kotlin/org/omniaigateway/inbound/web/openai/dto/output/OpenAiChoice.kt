package org.omniaigateway.inbound.web.openai.dto.output

data class OpenAiChoice(
    val index: Int,
    val message: OpenAiMessageOutput? = null,
    val delta: OpenAiDelta? = null,
    val finishReason: String? = null,
)
