package org.omniaigateway.inbound.web.anthropic.dto.output

import org.omniaigateway.adapters.anthropic.AnthropicResponseTranslator
import org.omniaigateway.domain.responses.CommonResponse
import org.omniaigateway.inbound.web.DomainMappableOut

data class AnthropicMessageResponse(
    val id: String,
    val type: String,
    val role: String,
    val model: String,
    val content: List<AnthropicOutputContent> = emptyList(),
    val stopReason: String? = null,
    val stopSequence: String? = null,
    val usage: AnthropicUsage? = null
) {
    companion object : DomainMappableOut<CommonResponse, AnthropicMessageResponse> {
        private val translator = AnthropicResponseTranslator()

        override fun fromDomain(domain: CommonResponse): AnthropicMessageResponse {
            val mapped = translator.fromDomain(domain)

            return AnthropicMessageResponse(
                id = mapped.id,
                type = mapped.type,
                role = mapped.role,
                model = mapped.model,
                content = mapped.content,
                stopReason = mapped.stopReason,
                stopSequence = mapped.stopSequence,
                usage = mapped.usage
            )
        }
    }
}

