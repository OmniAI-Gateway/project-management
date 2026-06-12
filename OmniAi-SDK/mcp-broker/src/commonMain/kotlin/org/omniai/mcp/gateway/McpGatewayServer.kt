package org.omniai.mcp.gateway

import io.ktor.client.HttpClient
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.server.StdioServerTransport
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import io.modelcontextprotocol.kotlin.sdk.types.ServerCapabilities
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.io.Sink
import kotlinx.io.Source
import org.omniai.mcp.domain.BrokerContext
import org.omniai.mcp.domain.BrokerServerClient
import org.omniai.mcp.domain.BrokerTool
import org.omniai.mcp.gateway.client.McpClientManager
import org.omniai.mcp.gateway.client.McpTransportFactory
import org.omniai.mcp.gateway.handler.ContextRegistrar
import org.omniai.mcp.gateway.handler.ProxyToolRegistrar
import org.omniai.mcp.gateway.handler.RestToolExecutor
import org.omniai.mcp.gateway.handler.ToolRegistrar
import org.omniai.mcp.gateway.mapping.ConfigMapper
import org.omniai.mcp.gateway.parsing.YamlConfigParser
import org.omniai.mcp.gateway.watcher.ConfigDirectoryWatcher

/**
 * The main entry point for the MCP Broker Gateway.
 *
 * Manages:
 * - Parsing and mapping YAML configurations
 * - Registering REST API tools on the MCP server
 * - Registering static contexts as MCP resources
 * - Connecting to external MCP servers and proxying their tools
 * - Hot-reloading when YAML files change in the config directory
 */
class McpGatewayServer(
    val name: String,
    val version: String,
    private val configDirectory: String,
    private val httpClient: HttpClient,
    private val transportFactory: McpTransportFactory,
    private val stdioInput: Source? = null,
    private val stdioOutput: Sink? = null
) {
    private val parser = YamlConfigParser()
    private val configMapper = ConfigMapper()
    private val watcher = ConfigDirectoryWatcher(configDirectory)
    private val restToolExecutor = RestToolExecutor(httpClient)
    private val toolRegistrar = ToolRegistrar(restToolExecutor)
    private val contextRegistrar = ContextRegistrar()
    private val clientManager = McpClientManager(transportFactory)
    private val proxyToolRegistrar = ProxyToolRegistrar(clientManager)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var mcpServer: Server? = null

    suspend fun start() {
        val server = Server(
            Implementation(name, version),
            ServerOptions(
                capabilities = ServerCapabilities(
                    tools = ServerCapabilities.Tools(listChanged = true),
                    resources = ServerCapabilities.Resources(listChanged = true, subscribe = false),
                    prompts = null
                )
            )
        )
        this.mcpServer = server

        // Load initial config
        val initialYamls = watcher.readAllYamlFiles()
        reloadConfig(server, initialYamls)

        // Start watcher for hot-reload
        watcher.startWatching { newYamls ->
            scope.launch {
                reloadConfig(server, newYamls)
            }
        }

        println("[McpGatewayServer] '$name' v$version started, watching '$configDirectory' for config changes")

        // Start transport — blocks until session ends
        val transport = StdioServerTransport(
            stdioInput ?: error("No Input provided for Stdio transport"),
            stdioOutput ?: error("No Output provided for Stdio transport")
        )
        server.createSession(transport)
    }

    suspend fun stop() {
        watcher.stopWatching()
        clientManager.disconnectAll()
        mcpServer?.close()
        println("[McpGatewayServer] '$name' stopped")
    }

    private suspend fun reloadConfig(server: Server, yamlContents: List<String>) {
        val allTools = mutableListOf<BrokerTool>()
        val allContexts = mutableListOf<BrokerContext>()
        val allServers = mutableListOf<BrokerServerClient>()

        for (yaml in yamlContents) {
            try {
                val dto = parser.parse(yaml)
                val mapped = configMapper.mapConfig(dto)
                allTools.addAll(mapped.tools)
                allContexts.addAll(mapped.contexts)
                allServers.addAll(mapped.servers)
            } catch (e: Exception) {
                println("[McpGatewayServer] Warning: Failed to parse YAML: ${e.message}")
            }
        }

        // Register REST API tools
        toolRegistrar.registerTools(server, allTools)

        // Register static contexts as resources
        contextRegistrar.registerContexts(server, allContexts)

        // Connect to external MCP servers and proxy their tools
        clientManager.syncConnections(allServers)
        proxyToolRegistrar.registerProxiedTools(server)

        println("[McpGatewayServer] Loaded: ${allTools.size} tools, ${allContexts.size} contexts, ${allServers.size} external servers")
    }
}
