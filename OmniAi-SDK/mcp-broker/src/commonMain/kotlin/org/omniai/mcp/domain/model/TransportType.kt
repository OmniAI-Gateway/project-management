package org.omniai.mcp.domain.model

import kotlinx.serialization.Serializable

/**
 * Transport types supported for MCP communication.
 * Shared between client and server configurations.
 */
@Serializable
enum class TransportType {
    STDIO, STREAMABLE_HTTP, WEBSOCKET, SSE
}
