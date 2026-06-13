package org.omniai.gateway.app

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered
import kotlinx.serialization.json.Json
import org.omniai.mcp.core.mcpGateway
import org.omniai.mcp.gateway.client.McpTransportFactory
import java.io.File

/**
 * Standalone entry point to start the MCP Broker inside the OmniAiGateway project.
 * It reads YAML configuration files from `mcp-configs` directory and runs as an MCP Server over STDIO.
 */
fun main() = runBlocking {
    val originalOut = System.out
    System.setOut(System.err)

    // Find 'mcp-configs' folder - works whether run from project root or OmniAiGateway subdir
    val workDir = File(System.getProperty("user.dir"))
    val configDir = listOf(
        File(workDir, "OmniAiGateway/mcp-configs"), // running from project root
        File(workDir, "mcp-configs"),                // running from OmniAiGateway dir
    ).firstOrNull { it.exists() && it.isDirectory }
        ?.absolutePath
        ?: File(workDir, "OmniAiGateway/mcp-configs").also { it.mkdirs() }.absolutePath

    val httpClient = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                explicitNulls = false
            })
        }
    }

    val gateway = mcpGateway {
        info("omniai-mcp-broker", "1.0.0")
        configDirectory(configDir)
        httpClient(httpClient)
        transportFactory(McpTransportFactory(httpClient))
        stdio(
            input = System.`in`.asSource().buffered(),
            output = originalOut.asSink().buffered()
        )
    }

    Runtime.getRuntime().addShutdownHook(Thread {
        runBlocking { gateway.stop() }
        httpClient.close()
    })

    gateway.start()
}
