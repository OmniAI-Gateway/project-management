package org.omniaigateway.inbound.web.anthropic.dto.output

import org.omniaigateway.domain.responses.CommonResponse
import org.omniaigateway.inbound.web.DomainMappableOut
import org.omniaigateway.inbound.web.anthropic.mapper.toAnthropicMessage

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
        override fun fromDomain(domain: CommonResponse): AnthropicMessageResponse =
            domain.toAnthropicMessage()
    }
}

