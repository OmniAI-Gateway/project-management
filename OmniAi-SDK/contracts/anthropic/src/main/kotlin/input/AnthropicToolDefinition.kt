package org.omniaigateway.contracts.anthropic.input

data class AnthropicToolDefinition(
    val name: String,
    val description: String,
    val inputSchema: Map<String, Any?>,
)
