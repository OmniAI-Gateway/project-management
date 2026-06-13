package org.omniai.mcp.domain

/**
 * Domain representation of an external MCP Server we are proxying to.
 */
data class BrokerServerClient(
    val name: String,
    val transport: TransportType,
    val command: String?,
    val args: List<String>,
    val url: String?
) {
    enum class TransportType {
        STDIO, SSE, WEBSOCKET
    }
}
