package org.omniai.mcp.server

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.response.respondText
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.sse.SSE
import io.ktor.server.sse.sse
import io.ktor.server.websocket.WebSockets
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.server.SseServerTransport
import io.modelcontextprotocol.kotlin.sdk.server.StdioServerTransport
import io.modelcontextprotocol.kotlin.sdk.server.mcpWebSocket
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import io.modelcontextprotocol.kotlin.sdk.types.ServerCapabilities
import kotlinx.coroutines.awaitCancellation

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
    private val sessions = mutableMapOf<String, SseServerTransport>()

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
                    intercept(io.ktor.server.application.ApplicationCallPipeline.Plugins) {
                        call.response.headers.append("Access-Control-Allow-Origin", "*")
                        call.response.headers.append("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
                        call.response.headers.append("Access-Control-Allow-Headers", "Content-Type, Authorization")
                        if (call.request.local.method == io.ktor.http.HttpMethod.Options) {
                            call.respondText("OK", status = HttpStatusCode.OK)
                            finish()
                        }
                    }

                    val currentRouting = this
                    for (transport in sseTransports) {
                        sse(transport.path) {
                            val session = this
                            val sseTransport = SseServerTransport("/message", session)
                            val sessionId = sseTransport.sessionId
                            this@BrokerServer.sessions[sessionId] = sseTransport
                            server.createSession(sseTransport)
                            try {
                                awaitCancellation()
                            } finally {
                                this@BrokerServer.sessions.remove(sessionId)
                                session.close()
                            }
                        }

                        post("/message") {
                            val sessionId = call.request.queryParameters["sessionId"]
                            val sseTransport = this@BrokerServer.sessions[sessionId]
                            if (sseTransport == null) {
                                call.respondText("Sessão não encontrada", status = HttpStatusCode.NotFound)
                                return@post
                            }
                            sseTransport.handlePostMessage(call)
                            call.respondText("Accepted", status = HttpStatusCode.Accepted)
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