package org.omniai.mcp.server

import io.ktor.server.application.Application
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.server.StdioServerTransport
import io.modelcontextprotocol.kotlin.sdk.server.mcp
import io.modelcontextprotocol.kotlin.sdk.server.mcpStreamableHttp
import io.modelcontextprotocol.kotlin.sdk.server.mcpWebSocket
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import io.modelcontextprotocol.kotlin.sdk.types.ServerCapabilities

/**
 * Manages the MCP Server instance and its transport sessions.
 * Supports multiple transports running simultaneously.
 */
class BrokerServer(
    name: String,
    version: String,
    val transports: List<ServerTransportConfig>,
    val application: Application?,
) {
    val server: Server =
        Server(
            Implementation(name, version),
            ServerOptions(
                capabilities =
                    ServerCapabilities(
                        tools = ServerCapabilities.Tools(listChanged = true),
                        resources = ServerCapabilities.Resources(listChanged = true, subscribe = false),
                        prompts = null,
                    ),
            ),
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
                    val stdioTransport =
                        StdioServerTransport(
                            input = transport.input,
                            output = transport.output,
                        )
                    server.createSession(stdioTransport)
                    println("[BrokerServer] STDIO transport session created")
                }

                is ServerTransportConfig.SSE -> {
                    application?.routing {
                        route(transport.path) {
                            this.mcp { server }
                        }
                    }
                }

                is ServerTransportConfig.StreamableHttp -> {
                    application?.mcpStreamableHttp(path = transport.path) { server }
                }

                is ServerTransportConfig.WebSocket -> {
                    application?.routing {
                        mcpWebSocket(transport.path, block = { server })
                    }
                }
            }
        }
    }
}
