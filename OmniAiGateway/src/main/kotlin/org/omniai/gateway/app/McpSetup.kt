package org.omniai.gateway.app

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import kotlinx.serialization.json.Json
import org.omniai.mcp.client.McpClientTransportFactory
import org.omniai.mcp.core.mcpBroker
import org.omniai.mcp.server.ServerTransportConfig
import java.io.File


suspend fun Application.buildMcpSetup() {
    val mcpWorkDir = File(System.getProperty("user.dir"))
    val mcpConfigDir = listOf(
        File(mcpWorkDir, "OmniAiGateway/mcp-configs"),
        File(mcpWorkDir, "mcp-configs")
    ).firstOrNull { it.exists() && it.isDirectory }?.absolutePath
        ?: File(mcpWorkDir, "OmniAiGateway/mcp-configs").also { it.mkdirs() }.absolutePath

    val mcpHttpClient = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                explicitNulls = false
            })
        }
    }

    val mcpBrokerServer = mcpBroker {
        info("omniai-mcp-broker", "1.0.0")
        configDirectory(mcpConfigDir)
        httpClient(mcpHttpClient)
        clientTransportFactory(McpClientTransportFactory(mcpHttpClient))
        
        application(this@buildMcpSetup)
        
//        serverTransport(
//            ServerTransportConfig.Stdio(
//                input = System.`in`.asSource().buffered(),
//                output = originalOut.asSink().buffered()
//            )
//        )
        serverTransport(
            ServerTransportConfig.Sse(path = "/sse")
        )
//        serverTransport(
//            ServerTransportConfig.WebSocket( path = "/ws")
//        )
    }

    mcpBrokerServer.start()

//    environment.monitor.subscribe(ApplicationStopped) {
//        runBlocking { mcpBrokerServer.stop() }
//        mcpHttpClient.close()
//    }
}
