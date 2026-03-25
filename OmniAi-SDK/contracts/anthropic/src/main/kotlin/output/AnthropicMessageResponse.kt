package org.omniaigateway.contracts.anthropic.output

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AnthropicMessageResponse(
    val id: String,
    val type: String,
    val role: String,
    val model: String,
    val content: List<AnthropicOutputContent> = emptyList(),
    @SerialName("stop_reason")
    val stopReason: String? = null,
    @SerialName("stop_sequence")
    val stopSequence: String? = null,
    val usage: AnthropicUsage? = null
) 
