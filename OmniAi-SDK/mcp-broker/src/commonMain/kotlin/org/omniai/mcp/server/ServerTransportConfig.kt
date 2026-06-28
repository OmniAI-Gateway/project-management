package org.omniai.mcp.server

import kotlinx.io.Sink
import kotlinx.io.Source

/**
 * Configuration for a broker server transport.
 * The broker can expose itself via multiple transports simultaneously.
 */
sealed class ServerTransportConfig {
    /**
     * STDIO transport - communicates via standard input/output streams.
     */
    data class Stdio(
        val input: Source,
        val output: Sink,
    ) : ServerTransportConfig()

    /**
     * Streamable HTTP transport - exposes the broker via HTTP Streamable endpoint.
     */
    data class StreamableHttp(
        val path: String = "/mcp",
    ) : ServerTransportConfig()

    /**
     * SSE transport - exposes the broker via SSE endpoint.
     */
    data class SSE(
        val path: String = "/sse",
    ) : ServerTransportConfig()

    /**
     * WebSocket transport - exposes the broker via WebSocket endpoint.
     */
    data class WebSocket(
        val path: String = "/ws",
    ) : ServerTransportConfig()
}
