package org.omniai.mcp.core

import io.ktor.client.HttpClient
import org.omniai.mcp.client.ClientTransportFactory
import org.omniai.mcp.client.McpClientManager
import org.omniai.mcp.config.mapping.ConfigMapper
import org.omniai.mcp.config.parsing.ConfigParser
import org.omniai.mcp.config.parsing.YamlConfigParser
import org.omniai.mcp.config.watcher.ConfigDirectoryWatcher
import org.omniai.mcp.handler.ContextRegistrar
import org.omniai.mcp.handler.ProxyToolRegistrar
import org.omniai.mcp.handler.RestToolExecutor
import org.omniai.mcp.handler.ToolRegistrar
import org.omniai.mcp.server.BrokerServer
import org.omniai.mcp.server.ServerTransportConfig

/**
 * DSL builder for creating and configuring an [McpBroker].
 */
class McpBrokerBuilder {
    var name: String = "mcp-broker"
    var version: String = "1.0.0"
    var configDirectory: String = "./config"

    private var httpClient: HttpClient? = null
    private var clientTransportFactory: ClientTransportFactory? = null
    private var configParser: ConfigParser = YamlConfigParser()
    private val serverTransports = mutableListOf<ServerTransportConfig>()

    fun info(name: String, version: String) {
        this.name = name
        this.version = version
    }

    fun configDirectory(path: String) {
        this.configDirectory = path
    }

    fun httpClient(client: HttpClient) {
        this.httpClient = client
    }

    fun clientTransportFactory(factory: ClientTransportFactory) {
        this.clientTransportFactory = factory
    }

    fun configParser(parser: ConfigParser) {
        this.configParser = parser
    }

    fun serverTransport(transport: ServerTransportConfig) {
        this.serverTransports.add(transport)
    }

    fun build(): McpBroker {
        val http = httpClient ?: error("HttpClient must be provided via httpClient(...)")
        val clientFactory = clientTransportFactory ?: error("ClientTransportFactory must be provided via clientTransportFactory(...)")

        require(serverTransports.isNotEmpty()) { "At least one server transport must be configured via serverTransport(...)" }

        val brokerServer = BrokerServer(
            name = name,
            version = version,
            transports = serverTransports.toList()
        )

        val configWatcher = ConfigDirectoryWatcher(configDirectory)
        val configMapper = ConfigMapper()
        val restToolExecutor = RestToolExecutor(http)
        val toolRegistrar = ToolRegistrar(restToolExecutor)
        val contextRegistrar = ContextRegistrar()
        val clientManager = McpClientManager(clientFactory)
        val proxyToolRegistrar = ProxyToolRegistrar(clientManager)

        return McpBroker(
            brokerServer = brokerServer,
            configParser = configParser,
            configMapper = configMapper,
            configWatcher = configWatcher,
            toolRegistrar = toolRegistrar,
            contextRegistrar = contextRegistrar,
            clientManager = clientManager,
            proxyToolRegistrar = proxyToolRegistrar
        )
    }
}

fun mcpBroker(block: McpBrokerBuilder.() -> Unit): McpBroker {
    return McpBrokerBuilder().apply(block).build()
}
