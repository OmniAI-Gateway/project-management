package org.omniai.mcp.server

import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.server.StdioServerTransport
import io.modelcontextprotocol.kotlin.sdk.server.mcp
import io.modelcontextprotocol.kotlin.sdk.server.mcpWebSocket
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import io.modelcontextprotocol.kotlin.sdk.types.ServerCapabilities

/**
 * Manages the MCP Server instance and its transport sessions.
 * Supports multiple transports running simultaneously.
 *
 * SSE and WebSocket transports use the SDK's built-in routing extensions
 * (mcp {} and mcpWebSocket {}) which handle session lifecycle, concurrency,
 * and cleanup internally — no manual Mutex or session tracking needed.
 */
class BrokerServer(
    name: String,
    version: String,
    val transports: List<ServerTransportConfig>,
    val application: Application?
) {

    val server: Server = Server(
        Implementation(name, version),
        ServerOptions(
            capabilities = ServerCapabilities(
                tools = ServerCapabilities.Tools(listChanged = true),
                resources = ServerCapabilities.Resources(listChanged = true, subscribe = false),
                prompts = null
            )
        )
    )

    init {
        require(transports.isNotEmpty()) { "At least one server transport must be configured" }
    }

    /**
     * Starts all configured transport sessions.
     */
    suspend fun start() {
        transports.forEach { transport ->
            when (transport) {
                is ServerTransportConfig.Stdio -> {
                    val stdioTransport = StdioServerTransport(
                        inputStream = transport.input,
                        outputStream = transport.output
                    )
                    server.connect(stdioTransport)
                    println("[BrokerServer] STDIO transport session created")
                }
                is ServerTransportConfig.Sse -> {
                    application?.routing {
                        mcp(transport.path) { server }
                    }
                    println("[BrokerServer] SSE transport registered on path ${transport.path}")
                }
                is ServerTransportConfig.WebSocket -> {
                    application?.routing {
                        mcpWebSocket(transport.path, block = { server })
                    }
                    println("[BrokerServer] WS transport registered on path ${transport.path}")
                }
            }
        }
    }
}