package org.omniaigateway.inbound.web.anthropic.dto.input

import org.omniaigateway.contracts.anthropic.input.AnthropicMessagesRequest as ContractAnthropicMessagesRequest
import org.omniaigateway.domain.requests.CommonRequest
import org.omniaigateway.inbound.anthropic.AnthropicInboundTranslator
import org.omniaigateway.inbound.web.DomainMappableIn

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
    override fun toDomain(): CommonRequest = translator.toDomain(toContract())

    private fun toContract(): ContractAnthropicMessagesRequest = ContractAnthropicMessagesRequest(
        model = model,
        maxTokens = maxTokens,
        messages = messages,
        system = system,
        tools = tools,
        toolChoice = toolChoice,
        stream = stream,
        temperature = temperature,
        topP = topP,
        topK = topK,
        stopSequences = stopSequences,
        stopToken = stopToken,
        thinking = thinking,
        metadata = metadata
    )

    private companion object {
        val translator = AnthropicInboundTranslator()
    }
}

