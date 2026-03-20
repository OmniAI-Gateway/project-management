package org.omniaigateway.inbound.web.openai.dto.output

import org.omniaigateway.inbound.web.openai.dto.input.OpenAiToolCall

data class OpenAiMessageOutput(
    val toolCalls: List<OpenAiToolCall>? = null,
    val content: String? = null,
    val role: String,
)



