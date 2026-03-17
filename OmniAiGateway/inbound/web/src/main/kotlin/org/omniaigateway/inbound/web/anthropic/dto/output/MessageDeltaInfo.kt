package org.omniaigateway.inbound.web.anthropic.dto.output

data class MessageDeltaInfo(
    val stopReason: String? = null,
    val stopSequence: String? = null
)

