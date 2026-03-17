package org.omniaigateway.inbound.web.anthropic.dto.input

data class AnthropicToolDefinition(
    val name: String,
    val description: String,
    val inputSchema: Map<String, Any?>,
)

