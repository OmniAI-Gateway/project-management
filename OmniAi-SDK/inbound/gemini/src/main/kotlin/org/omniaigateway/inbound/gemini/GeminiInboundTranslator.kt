package org.omniaigateway.inbound.gemini

import org.omniaigateway.core.ports.InboundTranslator
import org.omniaigateway.domain.common.CommonGenerationConfig
import org.omniaigateway.domain.common.CommonRole
import org.omniaigateway.domain.common.Provider
import org.omniaigateway.domain.common.content.TextPart
import org.omniaigateway.domain.requests.CommonRequest
import org.omniaigateway.domain.requests.CommonRequestMessage
import org.omniaigateway.inbound.web.gemini.dto.input.GeminiGenerateContentRequest

class GeminiInboundTranslator : InboundTranslator<GeminiGenerateContentRequest> {
    override val provider: Provider = Provider.GEMINI

    override fun toDomain(payload: GeminiGenerateContentRequest): CommonRequest = CommonRequest(
        provider = provider,
        model = "gemini-default",
        messages = payload.contents.map { content ->
            CommonRequestMessage(
                role = if (content.role.equals("model", ignoreCase = true)) CommonRole.ASSISTANT else CommonRole.USER,
                content = content.parts.mapNotNull { it.text?.let(::TextPart) }
            )
        },
        config = CommonGenerationConfig(
            temperature = payload.generationConfig?.temperature,
            maxTokens = null,
            topP = payload.generationConfig?.topP,
            stopSequences = payload.generationConfig?.stopSequences
        )
    )
}


