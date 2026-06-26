package org.omniai.mcp.client

import io.ktor.client.HttpClient
import io.modelcontextprotocol.kotlin.sdk.client.SseClientTransport
import io.modelcontextprotocol.kotlin.sdk.client.StdioClientTransport
import io.modelcontextprotocol.kotlin.sdk.client.WebSocketClientTransport
import io.modelcontextprotocol.kotlin.sdk.shared.Transport
import kotlinx.io.Sink
import kotlinx.io.Source
import org.omniai.mcp.domain.model.McpClientConfig
import org.omniai.mcp.domain.model.TransportType

/**
 * Expected function to launch a subprocess for STDIO communication.
 * Returns a pair of Source (input from process) and Sink (output to process).
 */
expect fun launchStdioProcess(command: String, args: List<String>): Pair<Source, Sink>

/**
 * Factory for creating MCP client transports based on server configuration.
 * Fully KMP compatible, uses Ktor for SSE and WebSocket.
 */
class McpClientTransportFactory(
    private val httpClient: HttpClient
) : ClientTransportFactory {
    override fun create(config: McpClientConfig): Transport {
        return when (config.transport) {
            TransportType.STDIO -> {
                val command = config.command
                    ?: error("STDIO transport requires a 'command' in server config '${config.name}'")
                val (source, sink) = launchStdioProcess(command, config.args)
                StdioClientTransport(input = source, output = sink)
            }
            TransportType.SSE -> {
                val url = config.url
                    ?: error("SSE transport requires a 'url' in server config '${config.name}'")
                SseClientTransport(client = httpClient, urlString = url)
            }
            TransportType.WEBSOCKET -> {
                val url = config.url
                    ?: error("WebSocket transport requires a 'url' in server config '${config.name}'")
                WebSocketClientTransport(client = httpClient, urlString = url)
            }
        }
    }
}
