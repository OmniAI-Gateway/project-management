package org.omniaigateway.inbound.web.openai.dto.output

import org.omniaigateway.inbound.web.openai.dto.input.OpenAiToolCall

data class OpenAiDelta(
    val role: String? = null,
    val content: String? = null,
    val toolCalls: List<OpenAiToolCall>? = null,
)
