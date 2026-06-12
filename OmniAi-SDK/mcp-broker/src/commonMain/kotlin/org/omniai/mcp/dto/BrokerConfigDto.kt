package org.omniai.mcp.dto

import kotlinx.serialization.Serializable

/**
 * Top-level YAML configuration DTO for the MCP Broker Gateway.
 * Represents the entire configuration parsed from a YAML file.
 */
@Serializable
data class BrokerConfigDto(
    val tools: List<ToolConfigDto> = emptyList(),
    val contexts: List<ContextConfigDto> = emptyList(),
    val mcpServers: List<McpServerConfigDto> = emptyList()
)
