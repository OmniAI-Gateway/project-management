package org.omniaigateway.contracts.anthropic.output

data class AnthropicMessageResponse(
    val id: String,
    val type: String,
    val role: String,
    val model: String,
    val content: List<AnthropicOutputContent> = emptyList(),
    val stopReason: String? = null,
    val stopSequence: String? = null,
    val usage: AnthropicUsage? = null
) 
