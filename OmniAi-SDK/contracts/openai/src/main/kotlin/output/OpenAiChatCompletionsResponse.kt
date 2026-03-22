package org.omniaigateway.contracts.openai.output

data class OpenAiChatCompletionsResponse(
    val id: String,
    val obj: String,
    val created: Long,
    val model: String,
    val systemFingerprint: String? = null,
    val choices: List<OpenAiChoice>,
    val usage: OpenAiUsage? = null,
)
