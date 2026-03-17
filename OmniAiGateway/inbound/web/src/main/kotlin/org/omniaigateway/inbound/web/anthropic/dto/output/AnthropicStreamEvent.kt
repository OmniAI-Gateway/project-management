package org.omniaigateway.inbound.web.anthropic.dto.output

sealed interface AnthropicStreamEvent {
    val type: String

    data class MessageStart(
        val message: AnthropicStreamMessage,
        override val type: String = "message_start"
    ) : AnthropicStreamEvent

    data class ContentBlockStart(
        val index: Int,
        val contentBlock: AnthropicOutputContent,
        override val type: String = "content_block_start"
    ) : AnthropicStreamEvent

    data class ContentBlockDelta(
        val index: Int,
        val delta: AnthropicStreamDelta,
        override val type: String = "content_block_delta"
    ) : AnthropicStreamEvent

    data class ContentBlockStop(
        val index: Int,
        override val type: String = "content_block_stop"
    ) : AnthropicStreamEvent

    data class MessageDelta(
        val delta: MessageDeltaInfo,
        val usage: AnthropicUsage? = null,
        override val type: String = "message_delta"
    ) : AnthropicStreamEvent

    data class MessageStop(
        override val type: String = "message_stop"
    ) : AnthropicStreamEvent

    data class Ping(
        override val type: String = "ping"
    ) : AnthropicStreamEvent

    data class Error(
        val error: AnthropicError,
        override val type: String = "error"
    ) : AnthropicStreamEvent
}

