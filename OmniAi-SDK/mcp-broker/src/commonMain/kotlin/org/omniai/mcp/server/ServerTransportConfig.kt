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
        val output: Sink
    ) : ServerTransportConfig()

    /**
     * SSE (Server-Sent Events) transport - exposes the broker via HTTP SSE endpoint.
     */
    data class Sse(
        val port: Int = 8080,
        val path: String = "/sse"
    ) : ServerTransportConfig()

    /**
     * WebSocket transport - exposes the broker via WebSocket endpoint.
     */
    data class WebSocket(
        val port: Int = 8080,
        val path: String = "/ws"
    ) : ServerTransportConfig()
}
