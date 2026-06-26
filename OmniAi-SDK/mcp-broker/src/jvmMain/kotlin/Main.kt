package org.omniai.mcp

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered
import kotlinx.serialization.json.Json
import org.omniai.mcp.client.McpClientTransportFactory
import org.omniai.mcp.core.mcpBroker
import org.omniai.mcp.server.ServerTransportConfig

/**
 * Entry point for the OmniAI MCP Broker.
 *
 * Starts the broker in STDIO mode, reading YAML configuration files
 * from the `gateway-config` directory in the project root.
 *
 * Usage:
 *   ./gradlew :OmniAi-SDK:mcp-broker:run
 *
 * Drop .yaml files into `mcp-broker/gateway-config/` to dynamically
 * register tools, contexts, and external MCP clients.
 */
fun main() = runBlocking {
    val configDir = System.getProperty("user.dir") + "/gateway-config"

    val httpClient = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                explicitNulls = false
            })
        }
    }

    val broker = mcpBroker {
        info("omniai-broker", "1.0.0")
        configDirectory(configDir)
        httpClient(httpClient)
        clientTransportFactory(McpClientTransportFactory(httpClient))
        serverTransport(
            ServerTransportConfig.Stdio(
                input = System.`in`.asSource().buffered(),
                output = System.out.asSink().buffered()
            )
        )
        serverTransport(
            ServerTransportConfig.Sse(port = 8080, path = "/sse")
        )
        serverTransport(
            ServerTransportConfig.WebSocket(port = 8080, path = "/ws")
        )
    }

    Runtime.getRuntime().addShutdownHook(Thread {
        runBlocking { broker.stop() }
        httpClient.close()
    })

    broker.start()
}
