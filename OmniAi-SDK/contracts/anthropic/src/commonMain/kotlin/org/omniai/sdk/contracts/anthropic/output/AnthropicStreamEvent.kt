package org.omniai.sdk.contracts.anthropic.output

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

@Serializable
@OptIn(ExperimentalSerializationApi::class)
@JsonClassDiscriminator("type")
sealed interface AnthropicStreamEvent {
    @Serializable
    @SerialName("message_start")
    data class MessageStart(
        val message: AnthropicMessageResponse,
    ) : AnthropicStreamEvent

    @Serializable
    @SerialName("content_block_start")
    data class ContentBlockStart(
        val index: Int,
        @SerialName("content_block")
        val contentBlock: AnthropicOutputContent,
    ) : AnthropicStreamEvent

    @Serializable
    @SerialName("content_block_delta")
    data class ContentBlockDelta(
        val index: Int,
        val delta: AnthropicStreamDelta,
    ) : AnthropicStreamEvent

    @Serializable
    @SerialName("content_block_stop")
    data class ContentBlockStop(
        val index: Int,
    ) : AnthropicStreamEvent

    @Serializable
    @SerialName("message_delta")
    data class MessageDelta(
        val delta: MessageDeltaInfo,
        val usage: AnthropicUsage? = null,
    ) : AnthropicStreamEvent

    @Serializable
    @SerialName("message_stop")
    object MessageStop : AnthropicStreamEvent

    @Serializable
    @SerialName("ping")
    object Ping : AnthropicStreamEvent

    @Serializable
    @SerialName("error")
    data class Error(
        val error: AnthropicError,
    ) : AnthropicStreamEvent
}
