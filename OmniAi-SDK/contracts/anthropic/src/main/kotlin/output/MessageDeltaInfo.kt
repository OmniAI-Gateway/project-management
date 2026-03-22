package org.omniaigateway.contracts.anthropic.output

data class MessageDeltaInfo(
    val stopReason: String? = null,
    val stopSequence: String? = null
)
