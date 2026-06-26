package org.omniai.mcp.config.dto

import kotlinx.serialization.Serializable
import org.omniai.mcp.domain.model.TransportType

/**
 * DTO for configuring an external MCP server to connect to as a client.
 */
@Serializable
data class McpClientConfigDto(
    val name: String,
    val transport: TransportType,
    val command: String? = null,
    val args: List<String> = emptyList(),
    val url: String? = null
)
