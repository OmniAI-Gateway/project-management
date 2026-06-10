package org.omniai.mcp.core

import org.omniai.mcp.capabilities.prompt.McpPrompt
import org.omniai.mcp.capabilities.prompt.PromptMessage
import org.omniai.mcp.capabilities.resource.McpResource
import org.omniai.mcp.capabilities.resource.ResourceContent
import org.omniai.mcp.capabilities.tool.McpTool
import org.omniai.mcp.config.parsing.YamlConfigParser
import org.omniai.mcp.config.registry.DefaultConfigMapper
import org.omniai.mcp.config.registry.DynamicToolRegistry
import org.omniai.mcp.transport.SseTransportConfig
import org.omniai.mcp.transport.StdioTransportConfig
import org.omniai.mcp.transport.TransportConfig
import org.omniai.mcp.transport.WebSocketTransportConfig
import org.omniai.sdk.ports.outbound.http.HttpTransportClient

class McpServerBuilder {
    var name: String = "mcp-server"
    var version: String = "1.0.0"

    private var transportConfig: TransportConfig? = null
    private val tools = mutableListOf<McpTool<*>>()
    private val resources = mutableListOf<McpResource>()
    private val prompts = mutableListOf<McpPrompt>()

    fun info(name: String, version: String) {
        this.name = name
        this.version = version
    }

    fun transport(block: TransportBuilder.() -> Unit) {
        transportConfig = TransportBuilder().apply(block).config
    }

    fun tools(block: ToolBuilder.() -> Unit) {
        ToolBuilder(tools).apply(block)
    }

    fun resources(block: ResourceBuilder.() -> Unit) {
        ResourceBuilder(resources).apply(block)
    }

    fun prompts(block: PromptBuilder.() -> Unit) {
        PromptBuilder(prompts).apply(block)
    }

    /**
     * Load tools and resources dynamically from a YAML configuration string.
     * Uses the shared [HttpTransportClient] for all HTTP execution.
     *
     * @param yamlContent Raw YAML configuration string defining tools and resources.
     * @param httpClient The HTTP client used to execute tool/resource requests at runtime.
     */
    fun loadDynamicConfig(
        yamlContent: String,
        httpClient: HttpTransportClient
    ) {
        val registry = DynamicToolRegistry(
            parser = YamlConfigParser(),
            configMapper = DefaultConfigMapper(),
            httpClient = httpClient
        )
        val result = registry.loadFromYaml(yamlContent)
        tools.addAll(result.tools)
        resources.addAll(result.resources)
    }

    fun build(): McpServer {
        requireNotNull(transportConfig) { "Transport must be configured" }
        return McpServer(name, version, transportConfig!!, tools, resources, prompts)
    }
}

class TransportBuilder {
    internal var config: TransportConfig? = null

    fun stdio() {
        config = StdioTransportConfig()
    }

    fun sse(port: Int, path: String, messagePath: String) {
        config = SseTransportConfig(port, path, messagePath)
    }

    fun websocket(port: Int, path: String) {
        config = WebSocketTransportConfig(port, path)
    }
}

class ToolBuilder(private val registry: MutableList<McpTool<*>>) {
    fun register(tool: McpTool<*>) {
        registry.add(tool)
    }
}

class ResourceBuilder(private val registry: MutableList<McpResource>) {
    fun register(
        uri: String,
        name: String,
        description: String? = null,
        mimeType: String? = null,
        handler: suspend (String) -> ResourceContent
    ) {
        registry.add(McpResource(uri, name, description, mimeType, handler))
    }
}

class PromptBuilder(private val registry: MutableList<McpPrompt>) {
    fun register(
        name: String,
        description: String? = null,
        handler: suspend (Map<String, String>?) -> List<PromptMessage>
    ) {
        registry.add(McpPrompt(name, description, handler))
    }
}

fun mcpServer(block: McpServerBuilder.() -> Unit): McpServer {
    return McpServerBuilder().apply(block).build()
}
