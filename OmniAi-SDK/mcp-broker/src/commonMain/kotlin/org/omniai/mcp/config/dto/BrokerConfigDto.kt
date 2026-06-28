package org.omniai.mcp.config.dto

import kotlinx.serialization.Serializable

/**
 * Top-level YAML configuration DTO for the MCP Broker.
 * Represents the entire configuration parsed from a YAML file.
 */
@Serializable
data class BrokerConfigDto(
    val tools: List<ToolConfigDto> = emptyList(),
    val contexts: List<ContextConfigDto> = emptyList(),
    val mcpServers: List<McpClientConfigDto> = emptyList(),
)
