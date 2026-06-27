package org.omniai.mcp.server

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.response.respondText
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.sse.sse
import io.ktor.utils.io.CancellationException
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.server.SseServerTransport
import io.modelcontextprotocol.kotlin.sdk.server.StdioServerTransport
import io.modelcontextprotocol.kotlin.sdk.server.mcpWebSocket
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import io.modelcontextprotocol.kotlin.sdk.types.ServerCapabilities
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.sync.Mutex

/**
 * Manages the MCP Server instance and its transport sessions.
 * Supports multiple transports running simultaneously.
 */
class BrokerServer(
    name: String,
    version: String,
    val transports: List<ServerTransportConfig>,
    val application: Application?
) {

    private val mutex = Mutex()
    private val sessions = mutableMapOf<String, SseServerTransport>()

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

    private suspend fun addSession(sessionId: String, transport: SseServerTransport) {
        mutex.lock()
        try {
            sessions[sessionId] = transport
        } finally {
            mutex.unlock()
        }
    }

    private suspend fun removeSession(sessionId: String) {
        mutex.lock()
        try {
            sessions.remove(sessionId)
        } finally {
            mutex.unlock()
        }
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
                        sse(transport.path) {
                            val session = this
                            val sseTransport = SseServerTransport("/message", session)
                            val sessionId = sseTransport.sessionId
                            addSession(sessionId, sseTransport)
                            val created = try {
                                server.connect(sseTransport)
                                true
                            } catch (e: CancellationException) {
                                println("cancelation exception")
                                throw e
                            } catch (e: Exception) {
                                removeSession(sessionId)
                                session.close()
                                false
                            }

                            if (!created) return@sse

                            try {
                                awaitCancellation()
                            } finally {
                                removeSession(sessionId)
                                session.close()
                            }
                        }
                        post("/message") {
                            val sessionId = call.request.queryParameters["sessionId"]
                            mutex.lock()
                            val sseTransport = this@BrokerServer.sessions[sessionId]
                            mutex.unlock()
                            if (sseTransport == null) {
                                call.respondText("Sessão não encontrada", status = HttpStatusCode.NotFound)
                                return@post
                            }
                            sseTransport.handlePostMessage(call)
                            call.respondText("Accepted", status = HttpStatusCode.Accepted)
                        }
                    }
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