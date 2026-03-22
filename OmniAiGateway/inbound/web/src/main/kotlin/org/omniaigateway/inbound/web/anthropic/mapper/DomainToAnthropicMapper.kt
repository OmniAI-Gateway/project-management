package org.omniaigateway.inbound.web.anthropic.mapper

import org.omniaigateway.adapters.anthropic.AnthropicResponseTranslator
import org.omniaigateway.domain.responses.CommonResponse
import org.omniaigateway.inbound.web.anthropic.dto.output.AnthropicMessageResponse

private val responseTranslator = AnthropicResponseTranslator()

fun CommonResponse.toAnthropicMessage(): AnthropicMessageResponse {
    val mapped = responseTranslator.fromDomain(this)

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

