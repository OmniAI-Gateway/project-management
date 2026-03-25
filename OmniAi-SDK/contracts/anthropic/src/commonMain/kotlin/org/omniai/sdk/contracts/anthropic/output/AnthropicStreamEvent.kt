package org.omniai.sdk.contracts.anthropic.output

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.omniai.sdk.contracts.anthropic.serialization.AnthropicStreamEventSerializer

@Serializable(with = AnthropicStreamEventSerializer::class)
sealed interface AnthropicStreamEvent {
    val type: String

    @Serializable
    data class MessageStart(
        val message: AnthropicMessageResponse,
        override val type: String = "message_start"
    ) : AnthropicStreamEvent

    @Serializable
    data class ContentBlockStart(
        val index: Int,
        @SerialName("content_block")
        val contentBlock: AnthropicOutputContent,
        override val type: String = "content_block_start"
    ) : AnthropicStreamEvent

    @Serializable
    data class ContentBlockDelta(
        val index: Int,
        val delta: AnthropicStreamDelta,
        override val type: String = "content_block_delta"
    ) : AnthropicStreamEvent

    @Serializable
    data class ContentBlockStop(
        val index: Int,
        override val type: String = "content_block_stop"
    ) : AnthropicStreamEvent

    @Serializable
    data class MessageDelta(
        val delta: MessageDeltaInfo,
        val usage: AnthropicUsage? = null,
        override val type: String = "message_delta"
    ) : AnthropicStreamEvent

    @Serializable
    data class MessageStop(
        override val type: String = "message_stop"
    ) : AnthropicStreamEvent

    @Serializable
    data class Ping(
        override val type: String = "ping"
    ) : AnthropicStreamEvent

    @Serializable
    data class Error(
        val error: AnthropicError,
        override val type: String = "error"
    ) : AnthropicStreamEvent
}
