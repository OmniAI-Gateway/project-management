package org.omniai.mcp.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.omniai.mcp.client.McpClientManager
import org.omniai.mcp.config.mapping.ConfigMapper
import org.omniai.mcp.config.parsing.ConfigParser
import org.omniai.mcp.config.watcher.ConfigDirectoryWatcher
import org.omniai.mcp.domain.model.ContextDefinition
import org.omniai.mcp.domain.model.McpClientConfig
import org.omniai.mcp.domain.model.RestToolDefinition
import org.omniai.mcp.handler.ContextRegistrar
import org.omniai.mcp.handler.ProxyToolRegistrar
import org.omniai.mcp.handler.ToolRegistrar
import org.omniai.mcp.server.BrokerServer

/**
 * The main orchestrator for the MCP Broker.
 *
 * Coordinates:
 * - Parsing and mapping YAML configurations
 * - Registering REST API tools on the MCP server
 * - Registering static contexts as MCP resources
 * - Connecting to external MCP servers and proxying their tools
 * - Hot-reloading when YAML files change in the config directory
 */
class McpBroker(
    private val onStop : () -> Unit = {},
    private val brokerServer: BrokerServer,
    private val configParser: ConfigParser,
    private val configMapper: ConfigMapper,
    private val configWatcher: ConfigDirectoryWatcher,
    private val toolRegistrar: ToolRegistrar,
    private val contextRegistrar: ContextRegistrar,
    private val clientManager: McpClientManager,
    private val proxyToolRegistrar: ProxyToolRegistrar
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    suspend fun start() {
        // Load initial config
        val initialYamls = configWatcher.readAllYamlFiles()
        reloadConfig(initialYamls)

        // Start watcher for hot-reload
        configWatcher.startWatching { newYamls ->
            scope.launch {
                try {
                    reloadConfig(newYamls)
                }catch (e: Exception){
                    e.printStackTrace()
                    throw  e
                }
            }
        }
        println("[McpBroker] Starting broker server...")
        brokerServer.start()
    }

    suspend fun stop() {
        configWatcher.stopWatching()
        clientManager.disconnectAll()
        scope.cancel()
        onStop()
        println("[McpBroker] Broker stopped")
    }

    private suspend fun reloadConfig(yamlContents: List<String>) {
        val allTools = mutableListOf<RestToolDefinition>()
        val allContexts = mutableListOf<ContextDefinition>()
        val allClients = mutableListOf<McpClientConfig>()

        for (yaml in yamlContents) {
            try {
                val dto = configParser.parse(yaml)
                val config = configMapper.mapConfig(dto)
                allTools.addAll(config.tools)
                allContexts.addAll(config.contexts)
                allClients.addAll(config.mcpClients)
            } catch (e: Exception) {
                println("[McpBroker] Warning: Failed to parse YAML: ${e.message}")
            }
        }

        val server = brokerServer.server

        // Register REST API tools
        toolRegistrar.registerTools(server, allTools)

        // Register static contexts as resources
        contextRegistrar.registerContexts(server, allContexts)

        // Connect to external MCP servers and proxy their tools
        clientManager.syncConnections(allClients)
        proxyToolRegistrar.registerProxiedTools(server)

        println("[McpBroker] Loaded: ${allTools.size} tools, ${allContexts.size} contexts, ${allClients.size} external MCP clients")
    }
}
