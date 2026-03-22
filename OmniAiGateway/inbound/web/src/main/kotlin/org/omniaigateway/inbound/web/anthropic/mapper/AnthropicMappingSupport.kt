package org.omniaigateway.inbound.web.anthropic.mapper

import org.omniaigateway.contracts.anthropic.input.AnthropicMessagesRequest
import org.omniaigateway.domain.requests.CommonRequest
import org.omniaigateway.inbound.anthropic.AnthropicInboundTranslator

private val inboundTranslator = AnthropicInboundTranslator()

// Backward-compatible helper in mapper package while translation logic lives in SDK inbound module.
fun AnthropicMessagesRequest.toDomain(): CommonRequest = inboundTranslator.toDomain(this)

