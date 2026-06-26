package org.omniai.mcp.server

import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.sse.SSE
import io.ktor.server.websocket.WebSockets
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
 */
class BrokerServer(
    name: String,
    version: String,
    private val transports: List<ServerTransportConfig>
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

    private var ktorServer: EmbeddedServer<*, *>? = null

    init {
        require(transports.isNotEmpty()) { "At least one server transport must be configured" }
    }

    /**
     * Starts all configured transport sessions.
     */
    suspend fun start() {
        val stdioTransports = transports.filterIsInstance<ServerTransportConfig.Stdio>()
        val sseTransports = transports.filterIsInstance<ServerTransportConfig.Sse>()
        val wsTransports = transports.filterIsInstance<ServerTransportConfig.WebSocket>()

        // 1. Setup STDIO transports
        for (transport in stdioTransports) {
            val stdioTransport = StdioServerTransport(
                inputStream = transport.input,
                outputStream = transport.output
            )
            server.createSession(stdioTransport)
            println("[BrokerServer] STDIO transport session created")
        }

        // 2. Setup HTTP Server for SSE and WS
        if (sseTransports.isNotEmpty() || wsTransports.isNotEmpty()) {
            val allHttpTransports = sseTransports + wsTransports
            val distinctPorts = allHttpTransports.map {
                when (it) {
                    is ServerTransportConfig.Sse -> it.port
                    is ServerTransportConfig.WebSocket -> it.port
                    else -> 8080
                }
            }.distinct()

            if (distinctPorts.size > 1) {
                println("[BrokerServer] Warning: Multiple ports requested. Binding to ${distinctPorts.first()} only.")
            }

            val bindPort = distinctPorts.firstOrNull() ?: 8080

            ktorServer = embeddedServer(CIO, port = bindPort) {
                install(SSE)
                install(WebSockets)

                routing {
                    val currentRouting = this@routing
                    for (transport in sseTransports) {
                        route(transport.path) {
                            mcp { server }
                        }
                        println("[BrokerServer] SSE transport registered on port $bindPort, path ${transport.path}")
                    }
                    for (transport in wsTransports) {
                        currentRouting.mcpWebSocket(transport.path, block = { server })
                        println("[BrokerServer] WS transport registered on port $bindPort, path ${transport.path}")
                    }
                }
            }
            ktorServer?.start(wait = false)
            println("[BrokerServer] Ktor Embedded Server listening on port $bindPort")
        }
    }

    /**
     * Stops the MCP server and all transport sessions.
     */
    suspend fun stop() {
        ktorServer?.stop(1000, 2000)
        server.close()
    }
}
