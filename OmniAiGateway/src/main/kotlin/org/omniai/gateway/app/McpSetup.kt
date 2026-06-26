package org.omniai.gateway.app

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered
import kotlinx.serialization.json.Json
import org.omniai.mcp.core.mcpGateway
import org.omniai.mcp.gateway.client.McpTransportFactory
import java.io.File
import java.io.PrintStream

fun Application.buildMcpSetup(originalOut: PrintStream) {
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

    val mcpGatewayServer = mcpGateway {
        info("omniai-mcp-broker", "1.0.0")
        configDirectory(mcpConfigDir)
        httpClient(mcpHttpClient)
        transportFactory(McpTransportFactory(mcpHttpClient))
        stdio(
            input = System.`in`.asSource().buffered(),
            output = originalOut.asSink().buffered()
        )
    }

    launch {
        mcpGatewayServer.start()
    }

    environment.monitor.subscribe(ApplicationStopped) {
        runBlocking { mcpGatewayServer.stop() }
        mcpHttpClient.close()
    }
}
