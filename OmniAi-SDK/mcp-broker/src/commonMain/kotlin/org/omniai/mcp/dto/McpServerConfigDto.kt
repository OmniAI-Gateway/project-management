package org.omniai.mcp.dto

import kotlinx.serialization.Serializable

/**
 * DTO for configuring an external MCP server to proxy to.
 */
@Serializable
data class McpServerConfigDto(
    val name: String,
    val transport: TransportType,
    val command: String? = null,
    val args: List<String> = emptyList(),
    val url: String? = null
) {
    @Serializable
    enum class TransportType {
        STDIO, SSE, WEBSOCKET
    }
}
