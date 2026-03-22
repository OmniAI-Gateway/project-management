package org.omniaigateway.inbound.openai

import org.omniaigateway.core.ports.InboundTranslator
import org.omniaigateway.domain.common.CommonGenerationConfig
import org.omniaigateway.domain.common.CommonRole
import org.omniaigateway.domain.common.Provider
import org.omniaigateway.domain.common.content.TextPart
import org.omniaigateway.domain.requests.CommonRequest
import org.omniaigateway.domain.requests.CommonRequestMessage
import org.omniaigateway.inbound.web.openai.dto.input.OpenAiChatCompletionsRequest
import org.omniaigateway.inbound.web.openai.dto.input.OpenAiStop

class OpenAiInboundTranslator : InboundTranslator<OpenAiChatCompletionsRequest> {
    override val provider: Provider = Provider.OPENAI

    override fun toDomain(payload: OpenAiChatCompletionsRequest): CommonRequest = CommonRequest(
        provider = provider,
        model = payload.model,
        messages = payload.messages.map { message ->
            CommonRequestMessage(
                role = message.role.toCommonRole(),
                content = message.content?.let(::TextPart)?.let(::listOf).orEmpty()
            )
        },
        config = CommonGenerationConfig(
            temperature = payload.temperature,
            maxTokens = payload.maxTokens,
            topP = payload.topP,
            stopSequences = payload.stop?.toStopSequences()
        ),
        providerOptions = mapOf(
            "stream" to payload.stream,
            "user" to payload.user,
            "seed" to payload.seed
        ).filterValues { it != null }
    )
}

private fun String.toCommonRole(): CommonRole = when (lowercase()) {
    "system" -> CommonRole.SYSTEM
    "assistant" -> CommonRole.ASSISTANT
    "tool" -> CommonRole.TOOL
    else -> CommonRole.USER
}

private fun OpenAiStop.toStopSequences(): List<String> = when (this) {
    is OpenAiStop.Single -> listOf(value)
    is OpenAiStop.Multiple -> values
}


