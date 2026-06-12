package org.omniai.mcp.gateway.handler

import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.serialization.json.buildJsonObject
import org.omniai.mcp.gateway.client.McpClientManager

/**
 * Discovers tools from remote MCP servers and registers them as proxy tools
 * on the local [Server].
 */
class ProxyToolRegistrar(
    private val clientManager: McpClientManager
) {
    /**
     * For each connected external MCP server, lists its tools and
     * registers a local proxy tool that forwards the call to the remote server.
     */
    suspend fun registerProxiedTools(server: Server) {
        for ((serverName, connection) in clientManager.getConnections()) {
            try {
                val remoteTools = connection.listTools()
                for (remoteTool in remoteTools.tools) {
                    val proxyName = "${serverName}_${remoteTool.name}"
                    server.addTool(
                        name = proxyName,
                        description = remoteTool.description ?: "Proxied from $serverName",
                        inputSchema = remoteTool.inputSchema
                    ) { request ->
                        try {
                            val arguments = request.params.arguments?.mapValues { it.value } ?: emptyMap()
                            connection.callTool(remoteTool.name, arguments)
                        } catch (e: Exception) {
                            CallToolResult(
                                content = listOf(TextContent("Error proxying to '$serverName/${remoteTool.name}': ${e.message}")),
                                isError = true
                            )
                        }
                    }
                }
                println("[ProxyToolRegistrar] Registered ${remoteTools.tools.size} proxied tools from '$serverName'")
            } catch (e: Exception) {
                println("[ProxyToolRegistrar] Failed to discover tools from '$serverName': ${e.message}")
            }
        }
    }
}
