package org.omniaigateway.inbound.web.openai.dto.output

data class OpenAiChatCompletionsResponse(
    val id: String,
    val `object`: String,
    val created: Long,
    val model: String,
    val systemFingerprint: String? = null,
    val choices: List<OpenAiChoice>,
    val usage: OpenAiUsage? = null,
)
