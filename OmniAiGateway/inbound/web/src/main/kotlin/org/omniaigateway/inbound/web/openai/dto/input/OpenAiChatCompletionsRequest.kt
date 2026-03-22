package org.omniaigateway.inbound.web.openai.dto.input

import org.omniaigateway.domain.common.CommonGenerationConfig
import org.omniaigateway.domain.common.Provider
import org.omniaigateway.domain.requests.CommonRequest
import org.omniaigateway.inbound.web.DomainMappableIn
import org.omniaigateway.inbound.web.openai.mapper.toDomainMessage
import org.omniaigateway.inbound.web.openai.mapper.toDomainTool
import org.omniaigateway.inbound.web.openai.mapper.toDomainToolChoice

data class OpenAiChatCompletionsRequest(
    val model: String,
    val messages: List<OpenAiMessageInput>,
    val temperature: Double? = null,
    val maxTokens: Int? = null,
    val topP: Double? = null,
    val stop: OpenAiStop? = null,
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
    val toolChoice: OpenAiToolChoice? = null,
) : DomainMappableIn<CommonRequest> {
    override fun toDomain(): CommonRequest {
        val providerOptions = buildMap<String, Any?> {
            if (stream != null) put("stream", stream)
            if (frequencyPenalty != null) put("frequencyPenalty", frequencyPenalty)
            if (presencePenalty != null) put("presencePenalty", presencePenalty)
            if (n != null) put("n", n)
            if (seed != null) put("seed", seed)
            if (user != null) put("user", user)
            if (logitBias != null) put("logitBias", logitBias)
            if (logprobs != null) put("logprobs", logprobs)
            if (topLogprobs != null) put("topLogprobs", topLogprobs)
            if (responseFormat != null) put("responseFormat", responseFormat)
        }

        return CommonRequest(
            provider = Provider.OPENAI,
            model = model,
            messages = messages.map { it.toDomainMessage() },
            config = CommonGenerationConfig(
                temperature = temperature,
                maxTokens = maxTokens,
                topP = topP,
                stopSequences = stop.toStopSequences()
            ),
            tools = tools?.map { it.toDomainTool() }.orEmpty(),
            toolChoice = toolChoice?.toDomainToolChoice(),
            jsonResponse = responseFormat?.type.equals("json_object", ignoreCase = true)
                || responseFormat?.type.equals("json_schema", ignoreCase = true),
            providerOptions = providerOptions
        )
    }
}

private fun OpenAiStop?.toStopSequences(): List<String>? =
    when (this) {
        is OpenAiStop.Single -> listOf(value)
        is OpenAiStop.Multiple -> values
        null -> null
    }
