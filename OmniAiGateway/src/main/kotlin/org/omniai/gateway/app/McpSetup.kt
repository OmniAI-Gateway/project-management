package org.omniai.gateway.app

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.modelcontextprotocol.kotlin.sdk.client.Client
import io.modelcontextprotocol.kotlin.sdk.client.StdioClientTransport
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered
import org.omniai.mcp.gateway.McpGatewayServer
import org.omniai.mcp.gateway.client.McpTransportFactory
import java.io.File
import java.io.PipedInputStream
import java.io.PipedOutputStream

data class McpSetupResult(
    val mcpClient: Client
)

/**
 * Initializes the in-process MCP setup.
 *
 * Creates a [McpGatewayServer] that reads tool definitions from the `mcp-configs/` directory
 * and connects it to a [Client] via in-memory pipes (no external sockets needed).
 *
 * Both the server and the client are started as background coroutines.
 *
 * @param configDir The path to the directory containing the MCP YAML config files.
 *                  Defaults to `<working-dir>/mcp-configs`.
 * @return A [McpSetupResult] containing the ready-to-use [Client] to pass to interceptors.
 */
fun buildMcpSetup(
    configDir: String = File(System.getProperty("user.dir"), "mcp-configs").absolutePath
): McpSetupResult {
    // Pipes que ligam in-process o McpGatewayServer ao Client (sem sockets externos)
    val serverToClientIn = PipedInputStream()
    val serverToClientOut = PipedOutputStream(serverToClientIn)
    val clientToServerIn = PipedInputStream()
    val clientToServerOut = PipedOutputStream(clientToServerIn)

    val mcpHttpClient = HttpClient(OkHttp)

    // O servidor MCP que lê os YAMLs da pasta mcp-configs/
    val mcpGatewayServer = McpGatewayServer(
        name = "omniai-gateway",
        version = "1.0.0",
        configDirectory = configDir,
        httpClient = mcpHttpClient,
        transportFactory = McpTransportFactory(mcpHttpClient),
        stdioInput = clientToServerIn.asSource().buffered(),
        stdioOutput = serverToClientOut.asSink().buffered()
    )

    // O cliente MCP que o interceptor vai usar para listar e chamar ferramentas
    val mcpClient = Client(
        clientInfo = Implementation(name = "omniai-gateway-client", version = "1.0.0")
    )
    val mcpTransport = StdioClientTransport(
        input = serverToClientIn.asSource().buffered(),
        output = clientToServerOut.asSink().buffered()
    )

    // Arrancar o servidor e o cliente MCP em background
    val mcpScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    mcpScope.launch { mcpGatewayServer.start() }
    mcpScope.launch { mcpClient.connect(mcpTransport) }

    return McpSetupResult(mcpClient = mcpClient)
}
