package org.omniai.mcp.client

import io.modelcontextprotocol.kotlin.sdk.client.Client
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import io.modelcontextprotocol.kotlin.sdk.types.ListToolsResult
import org.omniai.mcp.domain.model.McpClientConfig

/**
 * Wraps a connection to an external MCP server.
 * Manages the lifecycle (connect / disconnect) and proxies tool calls.
 */
class McpClientConnection(
    private val config: McpClientConfig,
    private val transportFactory: ClientTransportFactory,
    private val client: Client = Client(
        clientInfo = Implementation(
            name = "omniai-broker-client",
            version = "1.0.0"
        )
    )
) {
    val serverName: String get() = config.name

    private var connected = false

    /**
     * Connects to the external MCP server using the configured transport.
     */
    suspend fun connect() {
        val transport = transportFactory.create(config)
        client.connect(transport)
        connected = true
        println("[McpClientConnection] Connected to external server '${config.name}'")
    }

    /**
     * Lists all tools exposed by the remote MCP server.
     */
    suspend fun listTools(): ListToolsResult {
        check(connected) { "Not connected to server '${config.name}'" }
        return client.listTools()
    }

    /**
     * Calls a tool on the remote MCP server and returns the result.
     */
    suspend fun callTool(toolName: String, arguments: Map<String, Any?>): CallToolResult {
        check(connected) { "Not connected to server '${config.name}'" }
        return client.callTool(toolName, arguments)
    }

    /**
     * Disconnects from the external MCP server.
     */
    suspend fun disconnect() {
        if (connected) {
            client.close()
            connected = false
            println("[McpClientConnection] Disconnected from '${config.name}'")
        }
    }
}
