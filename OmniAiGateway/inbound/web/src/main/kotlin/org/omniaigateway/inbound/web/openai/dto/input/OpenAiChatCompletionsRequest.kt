package org.omniaigateway.inbound.web.openai.dto.input

data class OpenAiChatCompletionsRequest(
    val model: String,
    val messages: List<OpenAiMessageInput>,
    val temperature: Double? = null,
    val maxTokens: Int? = null,
    val topP: Double? = null,
    val stop: List<String>? = null,
    val frequencyPenalty: Double? = null,
    val presencePenalty: Double? = null,
    val n: Int? = null,
    val stream: Boolean? = null,
    val seed: Int? = null,
    val user: String? = null,
    val logitBias: Map<String, Int>? = null,
    val logprobs: Boolean? = null,
    val topLogprobs: Int? = null,
    val responseFormat: OpenAiResponseFormat? = null,
    val tools: List<OpenAiTool>? = null,
    val toolChoice: String? = null,
)
