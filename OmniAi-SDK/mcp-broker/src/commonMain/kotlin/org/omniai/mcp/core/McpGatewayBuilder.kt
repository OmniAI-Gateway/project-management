package org.omniai.mcp.core

import io.ktor.client.HttpClient
import kotlinx.io.Sink
import kotlinx.io.Source
import org.omniai.mcp.gateway.McpGatewayServer
import org.omniai.mcp.gateway.client.McpTransportFactory

/**
 * DSL builder for creating and configuring an [McpGatewayServer].
 */
class McpGatewayBuilder {
    var name: String = "mcp-gateway"
    var version: String = "1.0.0"
    var configDirectory: String = "./config"

    private var stdioInput: Source? = null
    private var stdioOutput: Sink? = null
    private var httpClient: HttpClient? = null
    private var transportFactory: McpTransportFactory? = null

    fun info(name: String, version: String) {
        this.name = name
        this.version = version
    }

    fun configDirectory(path: String) {
        this.configDirectory = path
    }

    fun stdio(input: Source, output: Sink) {
        this.stdioInput = input
        this.stdioOutput = output
    }

    fun httpClient(client: HttpClient) {
        this.httpClient = client
    }

    fun transportFactory(factory: McpTransportFactory) {
        this.transportFactory = factory
    }

    fun build(): McpGatewayServer {
        return McpGatewayServer(
            name = name,
            version = version,
            configDirectory = configDirectory,
            httpClient = httpClient ?: error("HttpClient must be provided via httpClient(...)"),
            transportFactory = transportFactory ?: error("McpTransportFactory must be provided via transportFactory(...)"),
            stdioInput = stdioInput,
            stdioOutput = stdioOutput
        )
    }
}

fun mcpGateway(block: McpGatewayBuilder.() -> Unit): McpGatewayServer {
    return McpGatewayBuilder().apply(block).build()
}
