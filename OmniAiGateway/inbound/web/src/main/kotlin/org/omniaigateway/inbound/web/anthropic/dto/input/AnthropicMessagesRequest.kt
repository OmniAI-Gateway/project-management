package org.omniaigateway.inbound.web.anthropic.dto.input

import org.omniaigateway.domain.common.CommonGenerationConfig
import org.omniaigateway.inbound.web.DomainMappableIn
import org.omniaigateway.domain.common.Provider
import org.omniaigateway.domain.common.SystemPrompt
import org.omniaigateway.domain.requests.CommonRequest
import org.omniaigateway.inbound.web.anthropic.mapper.toDomainMessage
import org.omniaigateway.inbound.web.anthropic.mapper.toDomainTool
import org.omniaigateway.inbound.web.anthropic.mapper.toDomainToolChoice

data class AnthropicMessagesRequest(
    val model: String,
    val maxTokens: Int,
    val messages: List<AnthropicMessageInput>,
    val system: String? = null,
    val tools: List<AnthropicToolDefinition>? = null,
    val toolChoice: AnthropicToolChoice? = null,
    val stream: Boolean? = null,
    val temperature: Double? = null,
    val topP: Double? = null,
    val topK: Int? = null,
    val stopSequences: List<String>? = null,
    val stopToken: String? = null,
    val thinking: AnthropicThinkingConfig? = null,
    val metadata: Map<String, Any?>? = null
) : DomainMappableIn<CommonRequest> {
    override fun toDomain(): CommonRequest {
        val providerOptions = buildMap<String, Any?> {
            if (stream != null) put("stream", stream)
            if (topK != null) put("topK", topK)
            if (stopToken != null) put("stopToken", stopToken)
            if (thinking != null) put("thinking", thinking)
            if (metadata != null) put("metadata", metadata)
        }

        return CommonRequest(
            provider = Provider.ANTHROPIC,
            model = model,
            messages = messages.map { it.toDomainMessage() },
            systemPrompt = system?.takeIf { it.isNotBlank() }?.let(::SystemPrompt),
            config = CommonGenerationConfig(
                temperature = temperature,
                maxTokens = maxTokens,
                topP = topP,
                stopSequences = stopSequences
            ),
            tools = tools?.map { it.toDomainTool() }.orEmpty(),
            toolChoice = toolChoice?.toDomainToolChoice(),
            jsonResponse = false,
            providerOptions = providerOptions
        )
    }
}

