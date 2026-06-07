package org.omniai.sdk.contracts.anthropic.input

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class AnthropicRole {
    @SerialName("user")
    USER,

    @SerialName("assistant")
    ASSISTANT
}

@Serializable
data class AnthropicMessageInput(
    val role: AnthropicRole,
    val content: AnthropicContent
)