package org.omniai.sdk.contracts.openai.input

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OpenAiChatCompletionsRequest(
    val model: String,
    val messages: List<OpenAiMessageInput>,
    val temperature: Double? = null,
    @SerialName("max_tokens")
    val maxTokens: Int? = null,
    @SerialName("top_p")
    val topP: Double? = null,
    val stop: OpenAiStop? = null,
    @SerialName("frequency_penalty")
    val frequencyPenalty: Double? = null,
    @SerialName("presence_penalty")
    val presencePenalty: Double? = null,
    val n: Int? = null,
    val stream: Boolean? = null,
    val seed: Int? = null,
    val user: String? = null,
    @SerialName("logit_bias")
    val logitBias: Map<String, Int>? = null,
    @SerialName("logprobs")
    val logProbs: Boolean? = null,
    @SerialName("top_logprobs")
    val topLogProbs: Int? = null,
    @SerialName("response_format")
    val responseFormat: OpenAiResponseFormat? = null,
    val tools: List<OpenAiTool>? = null,
    @SerialName("tool_choice")
    val toolChoice: OpenAiToolChoice? = null,
)
