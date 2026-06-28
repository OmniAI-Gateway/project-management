package org.omniai.mcp.domain.model

/**
 * Domain representation of an external MCP Server configuration
 * that the broker connects to as a client.
 */
data class McpClientConfig(
    val name: String,
    val transport: TransportType,
    val command: String?,
    val args: List<String>,
    val url: String?,
)
