package org.omniaigateway.contracts.anthropic.output

data class AnthropicUsage(
    val inputTokens: Int? = null,
    val outputTokens: Int? = null,
    val cacheCreationInputTokens: Int? = null,
    val cacheReadInputTokens: Int? = null,
    val serverToolUse: ServerToolUsage? = null
)
